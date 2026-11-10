package com.visual.backend.activos.dominio.modelo;

import java.util.Set;

/**
 * Estado de un activo. RESERVADO y EN_USO los gestionara el modulo de
 * logistica; este modulo controla mantenimiento y baja, y el modulo
 * financiero marca los equipos vendidos.
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
