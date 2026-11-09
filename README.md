# Visual COM: Sistema de Información Gerencial

Sistema de información gerencial para **Visual Com S.R.L.** (La Paz, Bolivia), empresa de alquiler y venta de equipo audiovisual (pantallas LED, sonido e iluminación) y de producción de eventos.

| Módulo | Estado |
|---|---|
| Administración de accesos (usuarios, roles, permisos, auditoría y respaldos) | ✅ Implementado |
| Gestión de activos e inventario (activos, lotes, uso, mantenimiento, módulos LED) | ✅ Implementado |
| Comercial y planificación de proyectos | Pendiente |
| Logística | Pendiente |
| Financiero | Pendiente |
| Información gerencial | Pendiente |
| Subcontratación y consumibles | Pendiente |

## Tecnologías

- **Backend** (`backend/`): Java 21, Spring Boot 4.1, Spring Security con JWT (resource server), Spring Data JPA/Hibernate y Flyway.
- **Frontend** (`frontend/`): Angular 22, Angular Material, SCSS y Vitest.
- **Base de datos**: PostgreSQL local, base `proyecto-visual-com`.

## Arquitectura (clean architecture)

Las dependencias apuntan siempre hacia el dominio.

```
backend/src/main/java/com/visual/backend/
├── compartido/                 Núcleo común: excepciones de negocio, paginación, manejo global de errores
├── activos/                    Activos por unidad y por lote, historial de uso, mantenimiento y módulos LED
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

Fuera de desarrollo, la cookie de refresco se marca como `Secure`, por lo que requiere HTTPS.

## Base de datos y migraciones

- `database/esquema_normalizado_4fn.sql` es el diseño original de referencia.
- Las migraciones de `backend/src/main/resources/db/migration` son la fuente de verdad:
  - `V1`: esquema base (copia del diseño original).
  - `V2`: ajustes de seguridad, tokens de refresco y tabla `auditoria` (inmutable mediante triggers).
  - `V3`: roles y permisos fijos con su asignación inicial.
  - `V4`: bodegas, tipo de control por modelo, historial de uso (vista `v_uso_activos` con horas acumuladas), existencias y movimientos por lote; tipos de mantenimiento, categorías y dos bodegas iniciales.
- `V5`: prefijo de código por modelo (`modelos_activo.prefijo_codigo`) para generar los códigos de inventario de sus unidades.
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

## Respaldos

Se generan con `pg_dump` en formato personalizado: uno automático cada día a las 02:00 y otros manuales desde **Administración de accesos → Respaldos**. Para restaurar uno:

```bash
pg_restore --clean -h localhost -U postgres -d proyecto-visual-com respaldo_AAAAMMDD_HHMMSS_mmm.dump
```

## Datos de prueba

El script `database/datos_prueba/01_accesos_activos.sql` carga datos similares a la operación real: personal con sus roles, modelos y equipos con códigos, uso en eventos, mantenimientos, módulos LED e inventario por lotes. Se ejecuta una sola vez sobre la base de desarrollo, con las migraciones ya aplicadas, y no modifica los datos existentes:

```bash
psql -h localhost -U postgres -d proyecto-visual-com -f database/datos_prueba/01_accesos_activos.sql
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
