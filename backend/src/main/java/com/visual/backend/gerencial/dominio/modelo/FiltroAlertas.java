package com.visual.backend.gerencial.dominio.modelo;

/** Criterios del listado de alertas; los nulos no filtran. */
public record FiltroAlertas(EstadoAlerta estado, TipoAlerta tipo, Severidad severidad) {
}
