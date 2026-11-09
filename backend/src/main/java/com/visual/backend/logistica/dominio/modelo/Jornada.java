package com.visual.backend.logistica.dominio.modelo;

/** Jornada de montaje o desmontaje de un plan logistico; id nulo si es nueva. */
public record Jornada(Integer id, TipoJornada tipo, Periodo periodo, String descripcion) {

    public Jornada {
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    public Jornada conId(Integer nuevoId) {
        return new Jornada(nuevoId, tipo, periodo, descripcion);
    }
}
