package com.visual.backend.gerencial.dominio.modelo;

/** Situaciones del estado de un activo que requieren la atencion del gerente. */
public enum TipoAlertaActivo {
    /** Supero el intervalo de horas de su mantenimiento preventivo. */
    MANTENIMIENTO_VENCIDO(Severidad.CRITICA),
    /** Alcanzo el porcentaje de alerta del intervalo de horas. */
    MANTENIMIENTO_PROXIMO(Severidad.ADVERTENCIA),
    /** Lleva en mantenimiento mas dias de lo esperado. */
    MANTENIMIENTO_PROLONGADO(Severidad.ADVERTENCIA),
    /** Tiene modulos LED defectuosos. */
    MODULOS_DEFECTUOSOS(Severidad.ADVERTENCIA),
    /** Acumula varios mantenimientos correctivos en el ultimo año. */
    FALLAS_RECURRENTES(Severidad.ADVERTENCIA),
    /** Disponible pero sin uso registrado en los ultimos dias. */
    SIN_USO(Severidad.INFORMATIVA);

    private final Severidad severidad;

    TipoAlertaActivo(Severidad severidad) {
        this.severidad = severidad;
    }

    public Severidad severidad() {
        return severidad;
    }
}
