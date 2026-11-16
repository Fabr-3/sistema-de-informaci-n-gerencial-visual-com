package com.visual.backend.activos.dominio.modelo;

import java.util.Set;

/**
 * Estado de un activo. Este modulo controla mantenimiento y baja, y el modulo
 * financiero marca los equipos vendidos. RESERVADO y EN_USO no se usan para
 * las reservas de logistica: un equipo puede tener varias reservas futuras,
 * por lo que su disponibilidad se deduce de las reservas activas.
 */
public enum EstadoOperativo {
    DISPONIBLE,
    RESERVADO,
    EN_USO,
    EN_MANTENIMIENTO,
    BAJA,
    VENDIDO;

    /** Estados de los equipos que ya no forman parte del inventario. */
    public static final Set<EstadoOperativo> FUERA_DE_INVENTARIO = Set.of(BAJA, VENDIDO);

    public boolean fueraDeInventario() {
        return FUERA_DE_INVENTARIO.contains(this);
    }
}
