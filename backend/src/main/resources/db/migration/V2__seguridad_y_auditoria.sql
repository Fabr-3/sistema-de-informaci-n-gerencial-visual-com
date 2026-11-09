-- =====================================================================
-- V2 - Ajustes del modulo de administracion de accesos.
--   * Roles con codigo estable (lo usa la aplicacion, el nombre es visible).
--   * Permisos agrupados por modulo.
--   * Usuarios: nombre de usuario para iniciar sesion, apellido materno
--     opcional, estado, vigencia (sub-contratados temporales), bloqueo por
--     intentos fallidos y cambio obligatorio de contrasena.
--   * Tokens de refresco (solo se guarda el hash, nunca el token).
--   * Auditoria inmutable de accesos y operaciones CRUD.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles y permisos
-- ---------------------------------------------------------------------
ALTER TABLE roles
    ADD COLUMN codigo VARCHAR(40) NOT NULL,
    ADD CONSTRAINT ux_roles_codigo UNIQUE (codigo),
    ADD CONSTRAINT ck_roles_codigo CHECK (codigo ~ '^[A-Z_]+$');

ALTER TABLE permisos
    ADD COLUMN modulo VARCHAR(50) NOT NULL,
    ADD CONSTRAINT ck_permisos_nombre CHECK (nombre ~ '^[A-Z_]+$');

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
ALTER TABLE usuarios
    ALTER COLUMN apellido_materno DROP NOT NULL,
    ADD COLUMN nombre_usuario          VARCHAR(50) NOT NULL,
    ADD COLUMN activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    -- Fecha en la que la cuenta deja de ser valida (obligatoria para
    -- sub-contratados temporales; se valida en la aplicacion). Para una
    -- recontratacion basta con ampliar esta fecha.
    ADD COLUMN fecha_expiracion        TIMESTAMP,
    ADD COLUMN intentos_fallidos       INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta         TIMESTAMP,
    ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fecha_ultimo_acceso     TIMESTAMP,
    ADD CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0),
    -- Se almacena en minusculas para evitar duplicados por mayusculas
    ADD CONSTRAINT ck_usuarios_nombre_usuario CHECK (nombre_usuario ~ '^[a-z0-9._-]{3,50}$');

CREATE UNIQUE INDEX ux_usuarios_nombre_usuario ON usuarios (nombre_usuario);
CREATE INDEX ix_usuarios_rol ON usuarios (id_rol);

-- ---------------------------------------------------------------------
-- Tokens de refresco de sesion
-- ---------------------------------------------------------------------
CREATE TABLE tokens_refresco (
    id_token          BIGSERIAL PRIMARY KEY,
    id_usuario        INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    hash_token        VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 en hexadecimal
    fecha_creacion    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion  TIMESTAMP NOT NULL,
    fecha_revocacion  TIMESTAMP,
    CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX ix_tokens_refresco_usuario ON tokens_refresco (id_usuario);

-- ---------------------------------------------------------------------
-- Auditoria
-- ---------------------------------------------------------------------
-- id_usuario es nulo en intentos de acceso con un usuario inexistente;
-- nombre_usuario guarda el identificador usado en ese momento.
CREATE TABLE auditoria (
    id_auditoria        BIGSERIAL PRIMARY KEY,
    fecha               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario          INT REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    nombre_usuario      VARCHAR(50),
    accion              VARCHAR(30) NOT NULL
                        CHECK (accion IN ('INICIO_SESION','INICIO_SESION_FALLIDO','CIERRE_SESION',
                                          'BLOQUEO_CUENTA','CAMBIO_CONTRASENA',
                                          'CREAR','ACTUALIZAR','ELIMINAR',
                                          'RESPALDO_CREADO','RESPALDO_DESCARGADO')),
    entidad             VARCHAR(100),
    id_entidad          VARCHAR(50),
    valores_anteriores  JSONB,
    valores_nuevos      JSONB,
    detalle             TEXT,
    direccion_ip        VARCHAR(45),
    agente_usuario      VARCHAR(255)
);
CREATE INDEX ix_auditoria_fecha   ON auditoria (fecha DESC);
CREATE INDEX ix_auditoria_usuario ON auditoria (id_usuario, fecha DESC);
CREATE INDEX ix_auditoria_entidad ON auditoria (entidad, id_entidad);

-- Los registros de auditoria se conservan de forma indefinida y no pueden
-- modificarse ni eliminarse.
CREATE OR REPLACE FUNCTION fn_auditoria_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoria no se pueden modificar ni eliminar'
        USING ERRCODE = 'insufficient_privilege';
END $$;

CREATE TRIGGER trg_auditoria_inmutable
BEFORE UPDATE OR DELETE ON auditoria
FOR EACH ROW EXECUTE FUNCTION fn_auditoria_inmutable();

CREATE TRIGGER trg_auditoria_sin_truncate
BEFORE TRUNCATE ON auditoria
FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_inmutable();
