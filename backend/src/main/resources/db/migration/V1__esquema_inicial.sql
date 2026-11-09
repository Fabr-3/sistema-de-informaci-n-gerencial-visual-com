-- =====================================================================
-- Sistema Gerencial Visual COM - Esquema normalizado (4FN)
-- Motor: PostgreSQL 15+ (el proyecto usa 17)
--
-- Convenciones:
--   * Catálogos mantenidos por el negocio: tabla propia (sin atributos
--     que dependan de otro atributo que no sea la clave).
--   * Estados controlados por el sistema: CHECK.
--   * Entidades maestras y transaccionales: auditoría estándar
--     (creado_por, fecha_creacion, modificado_por, fecha_modificacion,
--     fecha_eliminacion). Borrado lógico con fecha_eliminacion.
--   * Registros históricos, legales y financieros: ON DELETE RESTRICT.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- 1. Seguridad y acceso
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id_rol       SERIAL PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL UNIQUE,
    descripcion  TEXT
);

CREATE TABLE permisos (
    id_permiso   SERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL UNIQUE,
    descripcion  TEXT
);

CREATE TABLE roles_permisos (
    id_rol       INT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE,
    id_permiso   INT NOT NULL REFERENCES permisos(id_permiso) ON DELETE CASCADE,
    PRIMARY KEY (id_rol, id_permiso)
);

CREATE TABLE usuarios (
    id_usuario          SERIAL PRIMARY KEY,
    nombres             VARCHAR(100) NOT NULL,
    apellido_paterno    VARCHAR(100) NOT NULL,
    apellido_materno    VARCHAR(100) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    id_rol              INT NOT NULL REFERENCES roles(id_rol),
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP
);
-- El email no distingue mayúsculas: unicidad sobre lower(email)
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));

-- ---------------------------------------------------------------------
-- 2. Comercial: clientes, servicios, cotizaciones
-- ---------------------------------------------------------------------
-- Catálogo de tipos de documento de identidad (compartido por clientes,
-- proveedores y personal técnico). 'Otro' cubre cualquier documento no listado.
CREATE TABLE tipos_documento (
    id_tipo_documento  SERIAL PRIMARY KEY,
    nombre             VARCHAR(50) NOT NULL UNIQUE      -- Carnet de identidad, NIT, Pasaporte, Otro
);

-- Catálogo de tipos de cliente. Un cliente tiene exactamente un tipo
-- (dependencia funcional, no multivaluada), por lo que se guarda como FK.
CREATE TABLE tipos_cliente (
    id_tipo_cliente  SERIAL PRIMARY KEY,
    nombre           VARCHAR(50) NOT NULL UNIQUE          -- Natural, Empresarial, Estatal
);

CREATE TABLE clientes (
    id_cliente          SERIAL PRIMARY KEY,
    id_tipo_cliente     INT NOT NULL REFERENCES tipos_cliente(id_tipo_cliente),
    razon_social        VARCHAR(150) NOT NULL,           -- nombre completo si es persona natural
    direccion           TEXT,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP
);

-- 4FN: un cliente puede tener varios documentos de tipos distintos
-- (p. ej. CI del representante y NIT para facturar). El conjunto de tipos
-- es independiente del conjunto de números: cliente ->> tipo_documento | numero.
-- Se separa en una tabla propia en lugar de columnas (nit, ci, ...) en clientes.
-- Un cliente puede tener varios documentos; debe tener al menos uno activo
-- (ver trigger trg_cliente_min_documento).
CREATE TABLE documentos_cliente (
    id_documento_cliente  SERIAL PRIMARY KEY,
    id_cliente            INT NOT NULL REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    id_tipo_documento     INT NOT NULL REFERENCES tipos_documento(id_tipo_documento),
    numero                VARCHAR(50) NOT NULL,
    creado_por            INT REFERENCES usuarios(id_usuario),
    modificado_por        INT REFERENCES usuarios(id_usuario),
    fecha_creacion        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion    TIMESTAMP,
    fecha_eliminacion     TIMESTAMP,
    UNIQUE (id_tipo_documento, numero)                 -- un mismo documento no se registra dos veces
);
CREATE INDEX ix_documentos_cliente_cliente ON documentos_cliente (id_cliente);

-- Regla: todo cliente activo debe tener al menos un documento activo.
-- Es un trigger DIFERIDO: se evalúa al hacer COMMIT, por lo que el cliente y
-- sus documentos pueden insertarse dentro de la misma transacción.
CREATE OR REPLACE FUNCTION fn_cliente_requiere_documento() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_id_cliente INT;
    v_activo     BOOLEAN;
    v_docs       INT;
BEGIN
    IF TG_TABLE_NAME = 'clientes' THEN
        v_id_cliente := NEW.id_cliente;
    ELSE
        v_id_cliente := OLD.id_cliente;
    END IF;

    SELECT fecha_eliminacion IS NULL INTO v_activo
    FROM clientes WHERE id_cliente = v_id_cliente;

    -- Cliente inexistente (borrado en cascada) o dado de baja: no aplica
    IF NOT FOUND OR NOT v_activo THEN
        RETURN NULL;
    END IF;

    SELECT count(*) INTO v_docs
    FROM documentos_cliente
    WHERE id_cliente = v_id_cliente AND fecha_eliminacion IS NULL;

    IF v_docs = 0 THEN
        RAISE EXCEPTION 'El cliente % debe tener al menos un documento de identidad activo',
            v_id_cliente
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_cliente_min_documento
AFTER INSERT ON clientes
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_cliente_requiere_documento();

CREATE CONSTRAINT TRIGGER trg_documento_cliente_min
AFTER DELETE OR UPDATE OF id_cliente, fecha_eliminacion ON documentos_cliente
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_cliente_requiere_documento();

-- Catálogo de tipos de contacto (evita 'Celular Gerencia' como texto libre)
CREATE TABLE tipos_contacto (
    id_tipo_contacto    SERIAL PRIMARY KEY,
    nombre              VARCHAR(50) NOT NULL UNIQUE      -- Teléfono, Celular, Email
);

-- 4FN: cada contacto es un hecho independiente del cliente
CREATE TABLE contactos_cliente (
    id_contacto         SERIAL PRIMARY KEY,
    id_cliente          INT NOT NULL REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    id_tipo_contacto    INT NOT NULL REFERENCES tipos_contacto(id_tipo_contacto),
    valor               VARCHAR(150) NOT NULL,
    nombre_referencia   VARCHAR(100),                    -- Gerencia, Compras, ...
    UNIQUE (id_cliente, id_tipo_contacto, valor)
);

CREATE TABLE servicios (
    id_servicio         SERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL UNIQUE,
    descripcion         TEXT,
    precio_referencial  NUMERIC(10,2) CHECK (precio_referencial >= 0),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP
);

CREATE TABLE cotizaciones (
    id_cotizacion           SERIAL PRIMARY KEY,
    numero                  VARCHAR(20) NOT NULL UNIQUE,   -- correlativo
    id_cliente              INT NOT NULL REFERENCES clientes(id_cliente),
    fecha_emision           DATE NOT NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                            CHECK (estado IN ('BORRADOR','ENVIADA','ACEPTADA','RECHAZADA')),
    condiciones_comerciales TEXT,
    -- importe_total eliminado: es un dato derivado de cotizacion_servicios.
    -- Se consulta en v_cotizacion_importe.
    creado_por              INT REFERENCES usuarios(id_usuario),
    modificado_por          INT REFERENCES usuarios(id_usuario),
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion      TIMESTAMP,
    fecha_eliminacion       TIMESTAMP
);
CREATE INDEX ix_cotizaciones_cliente ON cotizaciones (id_cliente);

-- Línea de cotización. El precio_unitario se conserva porque es el precio
-- negociado en esa cotización, no el precio de catálogo (no es redundante).
CREATE TABLE cotizacion_servicios (
    id_cotizacion    INT NOT NULL REFERENCES cotizaciones(id_cotizacion) ON DELETE CASCADE,
    id_servicio      INT NOT NULL REFERENCES servicios(id_servicio),
    cantidad         INT NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    descuento        NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    PRIMARY KEY (id_cotizacion, id_servicio),
    CHECK (descuento <= cantidad * precio_unitario)
);

CREATE VIEW v_cotizacion_importe AS
SELECT c.id_cotizacion,
       COALESCE(SUM(cs.cantidad * cs.precio_unitario - cs.descuento), 0)::NUMERIC(12,2) AS importe_total
FROM cotizaciones c
LEFT JOIN cotizacion_servicios cs ON cs.id_cotizacion = c.id_cotizacion
GROUP BY c.id_cotizacion;

-- ---------------------------------------------------------------------
-- 3. Proyectos y contratos
-- ---------------------------------------------------------------------
CREATE TABLE proyectos (
    id_proyecto      SERIAL PRIMARY KEY,
    id_cotizacion    INT NOT NULL UNIQUE REFERENCES cotizaciones(id_cotizacion),  -- un proyecto por cotización
    nombre_proyecto  VARCHAR(200) NOT NULL,
    fecha_inicio     DATE,
    fecha_fin        DATE,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PLANIFICACION'
                     CHECK (estado IN ('PLANIFICACION','EN_EJECUCION','FINALIZADO','CANCELADO')),
    -- id_cliente no se repite: se obtiene vía cotizaciones (sin dependencia transitiva)
    creado_por       INT REFERENCES usuarios(id_usuario),
    modificado_por   INT REFERENCES usuarios(id_usuario),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    fecha_eliminacion  TIMESTAMP,
    CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE TABLE contratos (
    id_contrato      SERIAL PRIMARY KEY,
    id_proyecto      INT NOT NULL REFERENCES proyectos(id_proyecto) ON DELETE RESTRICT,
    numero           VARCHAR(30) NOT NULL UNIQUE,
    fecha_firma      DATE NOT NULL,
    archivo_adjunto  VARCHAR(255),
    terminos         TEXT,
    creado_por       INT REFERENCES usuarios(id_usuario),
    modificado_por   INT REFERENCES usuarios(id_usuario),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    fecha_eliminacion  TIMESTAMP
);
CREATE INDEX ix_contratos_proyecto ON contratos (id_proyecto);

-- ---------------------------------------------------------------------
-- 4. Eventos y lugares
-- ---------------------------------------------------------------------
-- La ubicación depende del lugar, no del evento: se extrae para no repetir
-- la dirección en cada evento que se realiza en el mismo sitio (3FN).
CREATE TABLE lugares (
    id_lugar            SERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    direccion           TEXT NOT NULL,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP,
    UNIQUE (nombre, direccion)
);

CREATE TABLE eventos (
    id_evento        SERIAL PRIMARY KEY,
    id_proyecto      INT NOT NULL REFERENCES proyectos(id_proyecto) ON DELETE RESTRICT,
    id_lugar         INT NOT NULL REFERENCES lugares(id_lugar),
    nombre_evento    VARCHAR(200) NOT NULL,
    fecha_inicio     TIMESTAMP NOT NULL,
    fecha_fin        TIMESTAMP NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO'
                     CHECK (estado IN ('PROGRAMADO','EJECUTADO','CANCELADO')),
    creado_por       INT REFERENCES usuarios(id_usuario),
    modificado_por   INT REFERENCES usuarios(id_usuario),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    fecha_eliminacion  TIMESTAMP,
    CHECK (fecha_fin > fecha_inicio)
);
CREATE INDEX ix_eventos_proyecto ON eventos (id_proyecto);
CREATE INDEX ix_eventos_lugar    ON eventos (id_lugar);

-- Regla de negocio que un CHECK no puede expresar (cruza tablas):
-- el evento debe quedar dentro del periodo del proyecto.
CREATE OR REPLACE FUNCTION fn_evento_dentro_del_proyecto() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_ini DATE;
    v_fin DATE;
BEGIN
    SELECT fecha_inicio, fecha_fin INTO v_ini, v_fin
    FROM proyectos WHERE id_proyecto = NEW.id_proyecto;

    IF (v_ini IS NOT NULL AND NEW.fecha_inicio::DATE < v_ini)
       OR (v_fin IS NOT NULL AND NEW.fecha_fin::DATE > v_fin) THEN
        RAISE EXCEPTION 'El evento % queda fuera del periodo del proyecto %',
            NEW.nombre_evento, NEW.id_proyecto
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_evento_dentro_del_proyecto
BEFORE INSERT OR UPDATE OF id_proyecto, fecha_inicio, fecha_fin ON eventos
FOR EACH ROW EXECUTE FUNCTION fn_evento_dentro_del_proyecto();

-- Servicios requeridos por un evento (M:N independiente de cotizacion_servicios)
CREATE TABLE evento_servicios (
    id_evento    INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,
    id_servicio  INT NOT NULL REFERENCES servicios(id_servicio),
    cantidad     INT NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_evento, id_servicio)
);

-- ---------------------------------------------------------------------
-- 5. Activos audiovisuales
-- ---------------------------------------------------------------------
CREATE TABLE categorias_activo (
    id_categoria  SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL UNIQUE          -- Pantalla LED, Proyector, ...
);

CREATE TABLE fabricantes (
    id_fabricante  SERIAL PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL UNIQUE
);

-- El modelo determina categoría y fabricante. Se separa para eliminar la
-- dependencia transitiva activo -> modelo -> categoría.
CREATE TABLE modelos_activo (
    id_modelo      SERIAL PRIMARY KEY,
    id_fabricante  INT NOT NULL REFERENCES fabricantes(id_fabricante),
    id_categoria   INT NOT NULL REFERENCES categorias_activo(id_categoria),
    nombre         VARCHAR(100) NOT NULL,
    UNIQUE (id_fabricante, nombre)
);

-- Especificaciones a nivel de MODELO: se repetían por unidad en
-- caracteristicas_activo. Ahora se guardan una sola vez por modelo.
CREATE TABLE especificaciones_modelo (
    id_modelo       INT NOT NULL REFERENCES modelos_activo(id_modelo) ON DELETE CASCADE,
    caracteristica  VARCHAR(100) NOT NULL,              -- Resolución, Brillo, Consumo (W)
    valor           VARCHAR(150) NOT NULL,
    PRIMARY KEY (id_modelo, caracteristica)
);

CREATE TABLE activos (
    id_activo          SERIAL PRIMARY KEY,
    codigo_inventario  VARCHAR(50) NOT NULL UNIQUE,
    id_modelo          INT NOT NULL REFERENCES modelos_activo(id_modelo),
    numero_serie       VARCHAR(100),
    estado_operativo   VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE'
                       CHECK (estado_operativo IN ('DISPONIBLE','RESERVADO','EN_USO','EN_MANTENIMIENTO','BAJA')),
    creado_por         INT REFERENCES usuarios(id_usuario),
    modificado_por     INT REFERENCES usuarios(id_usuario),
    fecha_creacion     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    fecha_eliminacion  TIMESTAMP,
    UNIQUE (id_modelo, numero_serie)                    -- NULL se permite repetido
);
CREATE INDEX ix_activos_modelo ON activos (id_modelo);

-- Módulo LED: pertenece a un activo padre (normalmente una pantalla).
CREATE TABLE modulos_led (
    id_modulo        SERIAL PRIMARY KEY,
    id_activo_padre  INT NOT NULL REFERENCES activos(id_activo) ON DELETE RESTRICT,
    codigo_modulo    VARCHAR(50) NOT NULL UNIQUE,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                     CHECK (estado IN ('ACTIVO','DEFECTUOSO','RETIRADO')),
    creado_por       INT REFERENCES usuarios(id_usuario),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP
);
CREATE INDEX ix_modulos_led_padre ON modulos_led (id_activo_padre);

-- ---------------------------------------------------------------------
-- 6. Logística, reservas y personal técnico
-- ---------------------------------------------------------------------
-- Un plan logístico por evento (supuesto a confirmar).
CREATE TABLE planes_logisticos (
    id_plan              SERIAL PRIMARY KEY,
    id_evento            INT NOT NULL UNIQUE REFERENCES eventos(id_evento) ON DELETE RESTRICT,
    detalles_transporte  TEXT,
    fecha_montaje        TIMESTAMP,
    fecha_desmontaje     TIMESTAMP,
    creado_por           INT REFERENCES usuarios(id_usuario),
    modificado_por       INT REFERENCES usuarios(id_usuario),
    fecha_creacion       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion   TIMESTAMP,
    fecha_eliminacion    TIMESTAMP,
    CHECK (fecha_desmontaje IS NULL OR fecha_montaje IS NULL OR fecha_desmontaje > fecha_montaje)
);

-- Vínculo plan <-> activo. Faltaba en la propuesta y es el núcleo de
-- "planificación de eventos y reserva de activos".
-- 4FN: NO fusionar con asignaciones_personal ni con servicios en una sola
-- tabla (plan ->> activo | plan ->> personal sería una violación de 4FN).
CREATE TABLE reservas_activo (
    id_plan         INT NOT NULL REFERENCES planes_logisticos(id_plan) ON DELETE CASCADE,
    id_activo       INT NOT NULL REFERENCES activos(id_activo) ON DELETE RESTRICT,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por      INT REFERENCES usuarios(id_usuario),
    PRIMARY KEY (id_plan, id_activo)
);
CREATE INDEX ix_reservas_activo_activo ON reservas_activo (id_activo);

CREATE TABLE personal_tecnico (
    id_personal          SERIAL PRIMARY KEY,
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    creado_por           INT REFERENCES usuarios(id_usuario),
    modificado_por       INT REFERENCES usuarios(id_usuario),
    fecha_creacion       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion   TIMESTAMP,
    fecha_eliminacion    TIMESTAMP
);

-- 4FN: misma lógica que documentos_cliente (varios documentos de tipos distintos).
CREATE TABLE documentos_personal (
    id_documento_personal  SERIAL PRIMARY KEY,
    id_personal            INT NOT NULL REFERENCES personal_tecnico(id_personal) ON DELETE CASCADE,
    id_tipo_documento      INT NOT NULL REFERENCES tipos_documento(id_tipo_documento),
    numero                 VARCHAR(50) NOT NULL,
    creado_por             INT REFERENCES usuarios(id_usuario),
    modificado_por         INT REFERENCES usuarios(id_usuario),
    fecha_creacion         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion     TIMESTAMP,
    fecha_eliminacion      TIMESTAMP,
    UNIQUE (id_personal, id_tipo_documento),
    UNIQUE (id_tipo_documento, numero)
);

CREATE TABLE especialidades_tecnicas (
    id_especialidad  SERIAL PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL UNIQUE
);

-- 4FN: especialidades independientes entre sí
CREATE TABLE personal_especialidades (
    id_personal      INT NOT NULL REFERENCES personal_tecnico(id_personal) ON DELETE CASCADE,
    id_especialidad  INT NOT NULL REFERENCES especialidades_tecnicas(id_especialidad) ON DELETE CASCADE,
    PRIMARY KEY (id_personal, id_especialidad)
);

CREATE TABLE roles_evento (
    id_rol_evento  SERIAL PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL UNIQUE          -- Técnico de sonido, Rigger, ...
);

CREATE TABLE asignaciones_personal (
    id_asignacion           SERIAL PRIMARY KEY,
    id_plan                 INT NOT NULL REFERENCES planes_logisticos(id_plan) ON DELETE CASCADE,
    id_personal             INT NOT NULL REFERENCES personal_tecnico(id_personal) ON DELETE RESTRICT,
    id_rol_evento           INT NOT NULL REFERENCES roles_evento(id_rol_evento),
    fecha_inicio_actividad  TIMESTAMP NOT NULL,
    fecha_fin_actividad     TIMESTAMP NOT NULL,
    CHECK (fecha_fin_actividad > fecha_inicio_actividad),
    -- Una persona no puede tener dos actividades solapadas.
    -- Si el negocio lo permite, eliminar esta restricción.
    EXCLUDE USING gist (
        id_personal WITH =,
        tsrange(fecha_inicio_actividad, fecha_fin_actividad, '[)') WITH &&
    )
);
CREATE INDEX ix_asignaciones_plan ON asignaciones_personal (id_plan);

-- ---------------------------------------------------------------------
-- 7. Mantenimiento
-- ---------------------------------------------------------------------
CREATE TABLE tipos_mantenimiento (
    id_tipo_mantenimiento  SERIAL PRIMARY KEY,
    nombre                 VARCHAR(50) NOT NULL UNIQUE    -- Preventivo, Correctivo, ...
);

CREATE TABLE mantenimientos (
    id_mantenimiento       SERIAL PRIMARY KEY,
    id_activo              INT NOT NULL REFERENCES activos(id_activo) ON DELETE RESTRICT,
    id_tipo_mantenimiento  INT NOT NULL REFERENCES tipos_mantenimiento(id_tipo_mantenimiento),
    fecha_inicio           TIMESTAMP NOT NULL,
    fecha_fin              TIMESTAMP,
    descripcion            TEXT,
    observaciones          TEXT,
    condicion_final        VARCHAR(50),
    estado                 VARCHAR(20) NOT NULL DEFAULT 'EN_PROCESO'
                           CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
    creado_por             INT REFERENCES usuarios(id_usuario),
    modificado_por         INT REFERENCES usuarios(id_usuario),
    fecha_creacion         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion     TIMESTAMP,
    fecha_eliminacion      TIMESTAMP,
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CHECK ((estado = 'FINALIZADO') = (fecha_fin IS NOT NULL))
);
CREATE INDEX ix_mantenimientos_activo ON mantenimientos (id_activo, fecha_inicio);

-- ---------------------------------------------------------------------
-- 8. Proveedores y movimientos financieros
-- ---------------------------------------------------------------------
CREATE TABLE proveedores (
    id_proveedor        SERIAL PRIMARY KEY,
    razon_social        VARCHAR(150) NOT NULL,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP
);

-- 4FN: un proveedor puede tener varios documentos de tipos distintos (NIT y otros).
CREATE TABLE documentos_proveedor (
    id_documento_proveedor  SERIAL PRIMARY KEY,
    id_proveedor            INT NOT NULL REFERENCES proveedores(id_proveedor) ON DELETE CASCADE,
    id_tipo_documento       INT NOT NULL REFERENCES tipos_documento(id_tipo_documento),
    numero                  VARCHAR(50) NOT NULL,
    creado_por              INT REFERENCES usuarios(id_usuario),
    modificado_por          INT REFERENCES usuarios(id_usuario),
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion      TIMESTAMP,
    fecha_eliminacion       TIMESTAMP,
    UNIQUE (id_proveedor, id_tipo_documento),
    UNIQUE (id_tipo_documento, numero)
);

CREATE TABLE servicios_proveedor (
    id_servicio_prov      SERIAL PRIMARY KEY,
    id_proveedor          INT NOT NULL REFERENCES proveedores(id_proveedor) ON DELETE CASCADE,
    descripcion_servicio  VARCHAR(150) NOT NULL,
    UNIQUE (id_proveedor, descripcion_servicio)
);

-- La naturaleza (ingreso/egreso) depende del tipo, no del movimiento.
-- Guardarla en movimientos_financieros generaría una dependencia transitiva.
CREATE TABLE tipos_movimiento (
    id_tipo_movimiento  SERIAL PRIMARY KEY,
    nombre              VARCHAR(50) NOT NULL UNIQUE,
    naturaleza          VARCHAR(10) NOT NULL CHECK (naturaleza IN ('INGRESO','EGRESO'))
);

CREATE TABLE movimientos_financieros (
    id_movimiento       SERIAL PRIMARY KEY,
    id_proyecto         INT NOT NULL REFERENCES proyectos(id_proyecto) ON DELETE RESTRICT,
    id_tipo_movimiento  INT NOT NULL REFERENCES tipos_movimiento(id_tipo_movimiento),
    concepto            VARCHAR(200) NOT NULL,
    monto               NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    fecha_movimiento    DATE NOT NULL,
    id_proveedor        INT REFERENCES proveedores(id_proveedor),   -- obligatorio en EGRESO (validar en aplicación o trigger)
    estado              VARCHAR(12) NOT NULL DEFAULT 'REGISTRADO'
                        CHECK (estado IN ('REGISTRADO','ANULADO')),  -- movimientos anulados excluidos de los reportes
    motivo_anulacion    TEXT,
    creado_por          INT REFERENCES usuarios(id_usuario),
    modificado_por      INT REFERENCES usuarios(id_usuario),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  TIMESTAMP,
    fecha_eliminacion   TIMESTAMP,
    CHECK (estado = 'REGISTRADO' OR motivo_anulacion IS NOT NULL)
);
CREATE INDEX ix_movimientos_proyecto ON movimientos_financieros (id_proyecto, fecha_movimiento);

-- =====================================================================
-- Fin del esquema.
-- Fuera de DDL (reglas a implementar en aplicación o triggers):
--   * Asignación de personal dentro del periodo del plan logístico.
--   * Evento dentro del periodo del plan de montaje/desmontaje.
--   * Cotización ACEPTADA no editable (bloquear en servicio o trigger).
--   * Alertas de solape de activos (reservas vs. mantenimientos): se
--     detectan por consulta/alerta, no se bloquean en BD.
--   * Tipo de documento válido según tipo de cliente (p. ej. natural con
--     CI; empresarial/estatal con NIT). No se fuerza en BD porque una
--     persona natural también puede tener NIT para facturar.
-- =====================================================================
