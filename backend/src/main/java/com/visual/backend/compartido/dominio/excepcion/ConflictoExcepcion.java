package com.visual.backend.compartido.dominio.excepcion;

import java.util.List;

/**
 * La operacion choca con datos existentes (por ejemplo, reservas del mismo
 * activo). Lleva el detalle de cada conflicto para mostrarlo al usuario.
 */
public class ConflictoExcepcion extends ReglaNegocioExcepcion {

    private final transient List<?> conflictos;

    public ConflictoExcepcion(String mensaje, List<?> conflictos) {
        super(mensaje);
        this.conflictos = List.copyOf(conflictos);
    }

    public List<?> getConflictos() {
        return conflictos;
    }
}
