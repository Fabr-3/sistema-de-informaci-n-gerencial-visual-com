# Visual COM: Sistema de Información Gerencial

Sistema de información gerencial para **Visual Com S.R.L.** (La Paz, Bolivia), empresa de alquiler y venta de equipo audiovisual (pantallas LED, sonido e iluminación) y de producción de eventos.

| Módulo | Estado |
|---|---|
| Administración de accesos (usuarios, roles, permisos, auditoría y respaldos) | ✅ Implementado |
| Gestión de activos e inventario (activos, lotes, uso, mantenimiento, módulos LED) | ✅ Implementado |
| Comercial y planificación de proyectos (clientes, cotizaciones, proyectos, eventos, contratos y cronograma) | ✅ Implementado |
| Logística (personal técnico, jornadas de montaje y desmontaje, asignación de personal) | ✅ Implementado |
| Financiero | Pendiente |
| Información gerencial | Pendiente |
| Subcontratación y consumibles | Pendiente |

## Tecnologías

- **Backend** (`backend/`): Java 21, Spring Boot 4.1, Spring Security con JWT (resource server), Spring Data JPA/Hibernate, Flyway y OpenPDF (cotizaciones y contratos en PDF).
- **Frontend** (`frontend/`): Angular 22, Angular Material, angular-calendar (cronograma), SCSS y Vitest.
- **Base de datos**: PostgreSQL local, base `proyecto-visual-com`.

## Arquitectura (clean architecture)

Las dependencias apuntan siempre hacia el dominio.

```
backend/src/main/java/com/visual/backend/
├── compartido/                 Núcleo común: excepciones de negocio, paginación, manejo global de errores y correlativos
├── activos/                    Activos por unidad y por lote, historial de uso, mantenimiento y módulos LED
├── comercial/                  Clientes, cotizaciones, proyectos, eventos, contratos y documentos PDF
├── logistica/                  Personal técnico, planes logísticos por evento y asignaciones de personal
├── accesos/
│   ├── dominio/                Modelos y reglas de negocio (sin Spring ni JPA) + puertos (interfaces)
│   ├── aplicacion/             Casos de uso (servicios) y DTO de entrada y salida
│   └── infraestructura/        Adaptadores: persistencia JPA, seguridad (JWT, BCrypt) y controladores REST
└── respaldos/                  Copias de seguridad con pg_dump (misma división en capas)

frontend/src/app/
├── nucleo/                     Configuración transversal, plantilla principal, menú y notificaciones
├── compartido/                 Componentes y utilidades reutilizables
└── modulos/<modulo>/
    ├── dominio/                Modelos y puertos (clases abstractas de repositorio)
    ├── aplicacion/             Casos de uso y estado (servicio de sesión con signals)
    ├── infraestructura/        Repositorios HTTP, interceptor y registro de dependencias
    └── presentacion/           Páginas, diálogos, guardias, directivas y rutas (carga diferida)
```

Para agregar un módulo nuevo:

1. Crear sus carpetas siguiendo la misma estructura.
2. Agregar sus migraciones de Flyway.
3. Registrar su grupo en `nucleo/diseno/menu.ts`.

## Puesta en marcha (desarrollo)

Requisitos: Java 21, Node 22+ con pnpm y PostgreSQL con la base `proyecto-visual-com` creada.

```bash
# Backend: aplica las migraciones y crea el administrador inicial al arrancar
cd backend && ./mvnw spring-boot:run          # http://localhost:8080

# Frontend: reenvía /api al backend (proxy.conf.json)
cd frontend && pnpm start                     # http://localhost:4200
```

**Credenciales iniciales (solo desarrollo):** usuario `admin`, contraseña `admin123456`.

### Configuración

El perfil `dev` (activo por defecto, en `application-dev.yaml`) trae valores locales. **En cualquier otro entorno** hay que definir estas variables:

| Variable | Descripción |
|---|---|
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión a PostgreSQL |
| `JWT_SECRETO` | Clave de firma HS256 (mínimo 32 caracteres, aleatoria) |
| `ADMIN_EMAIL`, `ADMIN_CONTRASENA_INICIAL` | Administrador que se crea si no existe ninguno |
| `RESPALDOS_DIRECTORIO` | Carpeta de respaldos (por defecto `./respaldos`) |
| `CONTRATOS_DIRECTORIO` | Carpeta de las copias firmadas de los contratos (por defecto `./documentos/contratos`) |

Los datos de la empresa que se imprimen en cotizaciones y contratos (razón social, dirección, ciudad, correo, teléfono y NIT) se configuran en `app.empresa` de `application.yaml`.

Fuera de desarrollo, la cookie de refresco se marca como `Secure`, por lo que requiere HTTPS.

## Base de datos y migraciones

- `database/esquema_normalizado_4fn.sql` es el diseño original de referencia.
- Las migraciones de `backend/src/main/resources/db/migration` son la fuente de verdad:
  - `V1`: esquema base (copia del diseño original).
  - `V2`: ajustes de seguridad, tokens de refresco y tabla `auditoria` (inmutable mediante triggers).
  - `V3`: roles y permisos fijos con su asignación inicial.
  - `V4`: bodegas, tipo de control por modelo, historial de uso (vista `v_uso_activos` con horas acumuladas), existencias y movimientos por lote; tipos de mantenimiento, categorías y dos bodegas iniciales.
  - `V5`: prefijo de código por modelo (`modelos_activo.prefijo_codigo`) para generar los códigos de inventario de sus unidades.
  - `V6`: módulo comercial. Estados de cotización (`BORRADOR`, `ACEPTADA`, `RECHAZADA`, `VENCIDA`), vigencia, título, periodo del evento y datos de la resolución; trigger que impide modificar las líneas de una cotización resuelta; unidad de medida de servicios, ciudad de lugares, descripción de proyectos y tipos iniciales de cliente, documento y contacto.
  - `V7`: logística. Jornadas de montaje y desmontaje (`jornadas_logisticas`, reemplazan a las fechas únicas del plan); se elimina la restricción `EXCLUDE` de `asignaciones_personal` porque los cruces solo se alertan; teléfono, estado y cuenta vinculada del personal técnico; especialidades y roles de evento iniciales; el gerente recibe `LOGISTICA_GESTIONAR`.
- Todo cambio de esquema va en una migración nueva. Nunca se edita una migración ya aplicada.

## Seguridad del módulo de accesos

- **Contraseñas:** se guardan con BCrypt (costo 12). La política exige de 8 a 72 caracteres, con al menos una letra y un número.
- **Bloqueo:** 5 intentos fallidos bloquean la cuenta durante 15 minutos.
- **Token de acceso:** es un JWT de 15 minutos y se guarda solo en memoria del navegador.
- **Token de refresco:** dura 7 días y viaja en una cookie `HttpOnly` y `SameSite=Strict`. Se rota en cada uso; si se reutiliza uno ya revocado, se cierran todas las sesiones del usuario.
- **Contraseña temporal:** los usuarios nuevos o restablecidos la reciben y no tienen ningún permiso hasta cambiarla.
- **Sub-contratado temporal:** requiere una fecha de vigencia. Para recontratarlo basta con ampliarla.
- **Protección de administradores:**
  - Siempre debe existir al menos un administrador habilitado.
  - El administrador no puede desactivarse, eliminarse ni cambiar su propio rol.
  - El rol Administrador conserva todos los permisos.
- **Auditoría:** registra accesos y operaciones CRUD con los valores anteriores y nuevos. Se conserva indefinidamente y la consultan el administrador y el gerente.

## Reglas del módulo de activos e inventario

- **Registro de unidades:** se pueden registrar varias unidades de un modelo en una sola operación (hasta 100). Cada unidad recibe un código correlativo con el prefijo del modelo (`ABPL-0001`, `ABPL-0002`, …); el código manual solo se admite al registrar una unidad. El prefijo es obligatorio para los modelos por unidad y no cambia una vez que el modelo tiene inventario. En Catálogos se muestra la cantidad en inventario de cada modelo.
- **Tipo de control:** lo define cada modelo.
  - **Por unidad:** equipos con código, serie, horas de uso y mantenimiento.
  - **Por lote:** cables y accesorios que se controlan por cantidad en cada bodega.
- **Alerta de mantenimiento preventivo:**
  - Las horas se suman a partir del historial de uso.
  - La alerta aparece al alcanzar el 90 % del intervalo del modelo (`app.activos.porcentaje-alerta-mantenimiento`) y se marca como vencida al 100 %.
  - Finalizar un mantenimiento de un tipo que reinicia horas (Preventivo) pone el contador en cero.
- **Historial de uso:** solo registra periodos pasados. Un activo no puede tener dos usos superpuestos, y un registro erróneo se anula sin borrarse.
- **Ciclo de mantenimiento:** iniciar un mantenimiento deja el activo *En mantenimiento*. Al finalizarlo vuelve a *Disponible*, salvo que se indique *No reparable*, en cuyo caso se da de baja.
- **Baja de activos:** solo se da de baja un activo *Disponible*. Un activo con historial no se elimina: se da de baja.
- **Módulos LED:** se rastrean uno por uno con código único y estado *Activo*, *Defectuoso* o *Retirado*.
- **Existencias por lote:** nunca quedan negativas y cada cambio queda como movimiento. Los traslados registran la salida y la entrada en una sola transacción. Una bodega inactiva solo admite salidas.
- **Permisos:**
  - `ACTIVOS_VER`: consultar.
  - `ACTIVOS_GESTIONAR`: registrar activos, catálogos y movimientos de lote.
  - `MANTENIMIENTOS_REGISTRAR` (técnicos): registrar uso y mantenimientos.

## Reglas del módulo comercial y de planificación

- **Clientes:** en las cotizaciones el cliente se busca por nombre o número de documento. Cada cliente tiene al menos un documento (CI o NIT) que no se repite entre clientes, y puede tener varios contactos. Un cliente con cotizaciones no se elimina.
- **Cotizaciones:**
  - Se numeran por año (`COT-2026-0001`) y su importe es la suma de sus líneas (cantidad × precio unitario − descuento). Solo admiten servicios activos.
  - Solo se editan en *Borrador*. Al resolverse, sus líneas quedan bloqueadas también en la base de datos.
  - El gerente las acepta o las rechaza (con motivo). Aceptar una cotización genera su proyecto con el importe contratado.
  - Un proceso diario (`app.comercial.cron-vencimiento-cotizaciones`, a las 00:10) marca como *Vencida* toda cotización en borrador cuya validez ya pasó.
  - Se descargan en PDF con los datos de la empresa.
- **Proyectos:** pasan de *Planificación* a *En ejecución* y de ahí a *Finalizado*; en cualquiera de los dos primeros estados se pueden cancelar. Solo un proyecto vigente (planificación o ejecución) admite eventos y contratos, y su periodo no puede cambiar si deja eventos programados fuera.
- **Eventos:** se programan en un lugar y dentro del periodo del proyecto (también validado por la base de datos). Un evento se marca *Ejecutado* solo después de haber comenzado; también puede cancelarse.
- **Contratos:** se numeran por año (`CTR-2026-0001`), su fecha de firma no puede ser futura y se generan en PDF a partir de la plantilla `backend/src/main/resources/plantillas/contrato-servicios.txt`. A cada contrato se le puede adjuntar la copia firmada escaneada (solo PDF, hasta 10 MB; se verifica el contenido del archivo). La copia se guarda en `CONTRATOS_DIRECTORIO` con el número del contrato y una nueva reemplaza a la anterior.
- **Cronograma:** muestra los eventos de todos los proyectos por mes, semana o día, con colores por estado. Al seleccionar un evento se abre su proyecto.
- **Permisos:**
  - `COMERCIAL_VER` (gerente): consultar clientes, cotizaciones, proyectos y cronograma.
  - `COMERCIAL_GESTIONAR` (administrador): registrar clientes, catálogos, cotizaciones, proyectos, eventos y contratos.
  - `COTIZACIONES_APROBAR` (gerente): aceptar o rechazar cotizaciones.

## Reglas del módulo de logística

- **Plan logístico:** cada evento tiene a lo sumo un plan, que se crea con su primer cambio (transporte, jornada o asignación). Solo se modifica mientras el evento está *Programado*; después queda para consulta.
- **Jornadas:** un evento puede tener varias jornadas de montaje y de desmontaje. El montaje termina antes del inicio del evento y el desmontaje comienza después de su fin, sin alejarse más de 30 días. Las jornadas de un plan no se cruzan entre sí. Si el evento cambia de horario, las jornadas desfasadas se muestran como aviso.
- **Ventana del plan:** va desde el primer montaje (o el inicio del evento) hasta el último desmontaje (o el fin del evento). Toda asignación debe quedar dentro, y una jornada no se puede quitar ni mover si deja asignaciones fuera.
- **Asignación de personal:** cada asignación tiene persona, rol y horario. Una persona no se repite en el mismo horario de un mismo evento, pero sí puede estar en varios eventos a la vez: el sistema muestra el cruce como alerta (en la lista de eventos, en el plan, al elegir a la persona y en «Mis asignaciones») y no lo bloquea. Los eventos cancelados no generan cruces.
- **Personal técnico:** documento de identidad único, especialidades y, opcionalmente, una cuenta de técnico o sub-contratado (una cuenta por persona). No se elimina: se desactiva, y solo si no tiene asignaciones pendientes. Al personal inactivo no se le asigna trabajo.
- **Mis asignaciones:** el técnico o sub-contratado con cuenta vinculada ve sus asignaciones desde hoy, con el lugar, el transporte y los cruces.
- **Permisos:**
  - `LOGISTICA_VER` (gerente): consultar planes y personal.
  - `LOGISTICA_GESTIONAR` (administrador y gerente): planificar jornadas, transporte y asignaciones, y registrar personal.
  - `ASIGNACIONES_VER_PROPIAS` (técnico y sub-contratado): consultar sus propias asignaciones.

## Respaldos

Se generan con `pg_dump` en formato personalizado: uno automático cada día a las 02:00 y otros manuales desde **Administración de accesos → Respaldos**. Las copias firmadas de los contratos no están en la base: hay que copiar también la carpeta `CONTRATOS_DIRECTORIO`. Para restaurar uno:

```bash
pg_restore --clean -h localhost -U postgres -d proyecto-visual-com respaldo_AAAAMMDD_HHMMSS_mmm.dump
```

## Datos de prueba

Los scripts de `database/datos_prueba/` cargan datos similares a la operación real. Se ejecutan una sola vez y en orden sobre la base de desarrollo, con las migraciones ya aplicadas, y no modifican los datos existentes:

- `01_accesos_activos.sql`: personal con sus roles, modelos y equipos con códigos, uso en eventos, mantenimientos, módulos LED e inventario por lotes.
- `02_comercial.sql`: servicios y lugares, ocho clientes con documentos y contactos, cotizaciones en todos los estados, proyectos con su cronograma de eventos y contratos.
- `03_logistica.sql`: nueve personas del personal técnico (cinco vinculadas a las cuentas de prueba), planes logísticos de eventos pasados y próximos con sus jornadas y asignaciones. Incluye un cruce a propósito: Daniela Gutiérrez está el 11/12 en dos eventos a la vez.

```bash
psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/01_accesos_activos.sql
psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/02_comercial.sql
psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/03_logistica.sql
```

Todos los usuarios de prueba tienen la contraseña `VisualCom2026`:

| Usuario | Rol | Nota |
|---|---|---|
| `mvargas`, `lquispe` | Gerente | |
| `jmamani`, `rcondori`, `dgutierrez` | Técnico | |
| `ftorrez` | Sub-contratado temporal | Vigente hasta el 31/03/2027 |
| `cmendoza` | Sub-contratado temporal | Vigencia vencida: no puede ingresar |

## Pruebas

```bash
cd backend && ./mvnw test        # unitarias e integración (usa la base proyecto_visual_com_test)
cd frontend && pnpm test         # Vitest
```

La base de pruebas debe existir antes de correrlas:

```bash
createdb -h localhost -U postgres proyecto_visual_com_test
```
