package com.visual.backend.gerencial.dominio.modelo;

/** Reportes operativos disponibles; el nombre de archivo lleva ademas el periodo. */
public enum TipoReporte {
    RESUMEN_GERENCIAL("Resumen gerencial", "resumen-gerencial"),
    EVENTOS("Eventos del periodo", "eventos"),
    ESTADO_ACTIVOS("Estado de activos e inventario", "estado-activos"),
    MANTENIMIENTOS("Mantenimientos de equipos", "mantenimientos"),
    PERSONAL("Utilización del personal técnico", "utilizacion-personal");

    private final String titulo;
    private final String archivo;

    TipoReporte(String titulo, String archivo) {
        this.titulo = titulo;
        this.archivo = archivo;
    }

    public String titulo() {
        return titulo;
    }

    public String archivo() {
        return archivo;
    }
}
