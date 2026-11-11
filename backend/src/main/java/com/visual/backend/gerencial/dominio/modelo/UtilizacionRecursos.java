package com.visual.backend.gerencial.dominio.modelo;

import java.util.List;

/**
 * Tasa de utilizacion de recursos: equipos controlados por unidad (dias con
 * uso registrado desde su adquisicion) y personal tecnico activo (dias con
 * asignaciones en eventos no cancelados).
 */
public record UtilizacionRecursos(TasaUtilizacion equipos, List<UtilizacionCategoria> porCategoria,
                                  TasaUtilizacion personal, List<UtilizacionPersonal> porTecnico) {

    public static UtilizacionRecursos de(List<UtilizacionCategoria> categorias, List<UtilizacionPersonal> tecnicos) {
        return new UtilizacionRecursos(TasaUtilizacion.total(categorias.stream().map(UtilizacionCategoria::tasa)
                .toList()), categorias, TasaUtilizacion.total(tecnicos.stream().map(UtilizacionPersonal::tasa)
                .toList()), tecnicos);
    }
}
