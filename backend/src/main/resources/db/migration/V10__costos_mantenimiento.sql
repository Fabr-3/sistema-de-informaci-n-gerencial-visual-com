-- =====================================================================
-- V10 - Costo de los mantenimientos en el modulo financiero.
--   * Al finalizar un mantenimiento con costo se registra automaticamente
--     un egreso general (sin proyecto) vinculado al mantenimiento.
--   * Los tipos con codigo de sistema solo los registra la aplicacion: no
--     se eligen al registrar un movimiento a mano (evita duplicar costos).
--   * Se registran los costos de los mantenimientos ya finalizados.
-- =====================================================================

ALTER TABLE tipos_movimiento
    ADD COLUMN codigo_sistema VARCHAR(30) UNIQUE;

INSERT INTO tipos_movimiento (nombre, naturaleza, requiere_proveedor, descuenta_inventario, codigo_sistema)
VALUES ('Mantenimiento de equipos', 'EGRESO', FALSE, FALSE, 'MANTENIMIENTO')
ON CONFLICT (nombre) DO UPDATE SET codigo_sistema = EXCLUDED.codigo_sistema;

ALTER TABLE movimientos_financieros
    ADD COLUMN id_mantenimiento INT UNIQUE REFERENCES mantenimientos(id_mantenimiento);

-- Costos de los mantenimientos finalizados antes de esta version
INSERT INTO movimientos_financieros (id_tipo_movimiento, concepto, monto, fecha_movimiento, id_mantenimiento,
                                     creado_por, fecha_creacion)
SELECT t.id_tipo_movimiento,
       LEFT('Mantenimiento ' || LOWER(tm.nombre) || ' · ' || a.codigo_inventario, 200),
       m.costo, m.fecha_fin::DATE, m.id_mantenimiento, m.modificado_por,
       COALESCE(m.fecha_modificacion, m.fecha_fin)
FROM mantenimientos m
JOIN tipos_mantenimiento tm ON tm.id_tipo_mantenimiento = m.id_tipo_mantenimiento
JOIN activos a ON a.id_activo = m.id_activo
JOIN tipos_movimiento t ON t.codigo_sistema = 'MANTENIMIENTO'
WHERE m.estado = 'FINALIZADO' AND m.fecha_eliminacion IS NULL AND m.costo > 0
ON CONFLICT (id_mantenimiento) DO NOTHING;
