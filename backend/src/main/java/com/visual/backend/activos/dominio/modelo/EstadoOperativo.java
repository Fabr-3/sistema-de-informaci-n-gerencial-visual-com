package com.visual.backend.activos.dominio.modelo;

/**
 * Estado de un activo. RESERVADO y EN_USO los gestionara el modulo de
 * logistica; este modulo controla mantenimiento y baja.
 */
public enum EstadoOperativo {
    DISPONIBLE,
    RESERVADO,
    EN_USO,
    EN_MANTENIMIENTO,
    BAJA
}
