package com.visual.backend.subcontratacion.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/** Criterios de busqueda del modulo de subcontratacion y consumibles. */
public final class Filtros {

    private Filtros() {
    }

    /** Subcontrataciones con fecha en [desde, hasta] (ambos opcionales). */
    public record Subcontrataciones(Integer idProyecto, Integer idEvento, Integer idProveedor, LocalDate desde,
                                    LocalDate hasta) {
    }

    public record Consumibles(String texto, Boolean activo, boolean soloStockBajo) {
    }

    /** idsEventos, si no es nulo, limita el listado a esos eventos (tecnicos). */
    public record Consumos(Integer idProyecto, Integer idEvento, Integer idConsumible, LocalDate desde,
                           LocalDate hasta, List<Integer> idsEventos) {

        public Consumos soloEventos(List<Integer> ids) {
            return new Consumos(idProyecto, idEvento, idConsumible, desde, hasta, List.copyOf(ids));
        }
    }
}
