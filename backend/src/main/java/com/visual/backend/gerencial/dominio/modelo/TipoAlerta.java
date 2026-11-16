package com.visual.backend.gerencial.dominio.modelo;

/** Situaciones de los activos y consumibles que requieren la atencion del gerente. */
public enum TipoAlerta {
    /** Equipo con una reserva para un evento proximo que esta en mantenimiento. */
    RESERVA_EN_RIESGO(Severidad.CRITICA),
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
    SIN_USO(Severidad.INFORMATIVA),
    /** Consumible con el stock en su minimo o por debajo. */
    STOCK_BAJO(Severidad.ADVERTENCIA);

    private final Severidad severidad;

    TipoAlerta(Severidad severidad) {
        this.severidad = severidad;
    }

    public Severidad severidad() {
        return severidad;
    }
}
