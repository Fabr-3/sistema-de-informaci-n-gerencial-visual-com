package com.visual.backend.gerencial.dominio.modelo;

import java.util.List;

/** Cantidad de alertas por severidad. */
public record ResumenAlertas(long criticas, long advertencias, long informativas) {

    public static ResumenAlertas de(List<AlertaActivo> alertas) {
        return new ResumenAlertas(contar(alertas, Severidad.CRITICA), contar(alertas, Severidad.ADVERTENCIA),
                contar(alertas, Severidad.INFORMATIVA));
    }

    public long total() {
        return criticas + advertencias + informativas;
    }

    private static long contar(List<AlertaActivo> alertas, Severidad severidad) {
        return alertas.stream().filter(alerta -> alerta.severidad() == severidad).count();
    }
}
