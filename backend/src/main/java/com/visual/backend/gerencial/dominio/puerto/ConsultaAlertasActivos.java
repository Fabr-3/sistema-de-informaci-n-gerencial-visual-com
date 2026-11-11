package com.visual.backend.gerencial.dominio.puerto;

import java.time.LocalDate;
import java.util.List;

import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;

/** Situaciones de los activos vigentes que generan alertas. */
public interface ConsultaAlertasActivos {

    /** Preventivos vencidos o proximos segun las horas de uso. */
    List<AlertaActivo> mantenimientoPreventivo();

    /** Mantenimientos en proceso iniciados en la fecha limite o antes. */
    List<AlertaActivo> mantenimientosProlongados(LocalDate iniciadosHasta, LocalDate hoy);

    List<AlertaActivo> modulosDefectuosos();

    /** Activos con al menos el minimo de mantenimientos correctivos iniciados desde la fecha. */
    List<AlertaActivo> fallasRecurrentes(int minimoCorrectivos, LocalDate desde);

    /** Equipos disponibles, adquiridos antes de la fecha, sin uso desde ella. */
    List<AlertaActivo> sinUso(LocalDate desde, LocalDate hoy);
}
