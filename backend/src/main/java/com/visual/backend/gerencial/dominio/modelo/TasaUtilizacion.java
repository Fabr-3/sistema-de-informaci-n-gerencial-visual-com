package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dias ocupados sobre dias disponibles de un conjunto de recursos. Un dia
 * cuenta como ocupado si el recurso tuvo uso o asignacion en algun momento
 * de ese dia. El porcentaje es nulo si no hubo dias disponibles.
 */
public record TasaUtilizacion(int recursos, long diasDisponibles, long diasOcupados, BigDecimal porcentaje) {

    public static TasaUtilizacion de(int recursos, long diasDisponibles, long diasOcupados) {
        return new TasaUtilizacion(recursos, diasDisponibles, diasOcupados,
                Porcentaje.de(diasOcupados, diasDisponibles));
    }

    public static TasaUtilizacion total(List<TasaUtilizacion> tasas) {
        return de(tasas.stream().mapToInt(TasaUtilizacion::recursos).sum(),
                tasas.stream().mapToLong(TasaUtilizacion::diasDisponibles).sum(),
                tasas.stream().mapToLong(TasaUtilizacion::diasOcupados).sum());
    }
}
