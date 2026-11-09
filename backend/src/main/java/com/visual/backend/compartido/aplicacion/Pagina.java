package com.visual.backend.compartido.aplicacion;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado independiente del mecanismo de persistencia. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public <R> Pagina<R> mapear(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(),
                pagina, tamano, totalElementos, totalPaginas);
    }
}
