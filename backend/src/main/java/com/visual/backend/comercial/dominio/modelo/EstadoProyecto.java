package com.visual.backend.comercial.dominio.modelo;

import java.util.Map;
import java.util.Set;

/** Ciclo de vida de un proyecto y las transiciones permitidas. */
public enum EstadoProyecto {
    PLANIFICACION,
    EN_EJECUCION,
    FINALIZADO,
    CANCELADO;

    private static final Map<EstadoProyecto, Set<EstadoProyecto>> TRANSICIONES = Map.of(
            PLANIFICACION, Set.of(EN_EJECUCION, CANCELADO),
            EN_EJECUCION, Set.of(FINALIZADO, CANCELADO),
            FINALIZADO, Set.of(),
            CANCELADO, Set.of());

    public boolean puedeCambiarA(EstadoProyecto destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    /** En planificacion o ejecucion se pueden programar eventos y firmar contratos. */
    public boolean estaVigente() {
        return this == PLANIFICACION || this == EN_EJECUCION;
    }
}
