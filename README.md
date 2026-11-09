# Visual COM: Sistema de Información Gerencial

Sistema de información gerencial para **Visual Com S.R.L.** (La Paz, Bolivia), empresa de alquiler y venta de equipo audiovisual (pantallas LED, sonido e iluminación) y de producción de eventos.

| Módulo | Estado |
|---|---|
| Administración de accesos (usuarios, roles, permisos, auditoría y respaldos) | ✅ Implementado |
| Gestión de activos e inventario | Pendiente |
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

## Respaldos

Se generan con `pg_dump` en formato personalizado: uno automático cada día a las 02:00 y otros manuales desde **Administración de accesos → Respaldos**. Para restaurar uno:

```bash
pg_restore --clean -h localhost -U postgres -d proyecto-visual-com respaldo_AAAAMMDD_HHMMSS_mmm.dump
```

## Pruebas

```bash
cd backend && ./mvnw test        # unitarias e integración (usa la base proyecto_visual_com_test)
cd frontend && pnpm test         # Vitest
```

La base de pruebas debe existir antes de correrlas:

```bash
createdb -h localhost -U postgres proyecto_visual_com_test
```
