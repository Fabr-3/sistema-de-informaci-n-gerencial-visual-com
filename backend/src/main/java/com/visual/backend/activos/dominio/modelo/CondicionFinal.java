package com.visual.backend.activos.dominio.modelo;

/** Resultado de un mantenimiento. NO_REPARABLE da de baja el activo. */
public enum CondicionFinal {
    OPERATIVO,
    OPERATIVO_CON_OBSERVACIONES,
    NO_REPARABLE
}
