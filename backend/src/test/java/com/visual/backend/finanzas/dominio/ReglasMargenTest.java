package com.visual.backend.finanzas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.visual.backend.finanzas.dominio.modelo.Margen;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;

class ReglasMargenTest {

    @Test
    void elMargenEsIngresosMenosCostosSobreLosIngresos() {
        Margen margen = Margen.calcular(new BigDecimal("38860.00"), new BigDecimal("12400.00"));

        assertThat(margen.margen()).isEqualByComparingTo("26460.00");
        assertThat(margen.porcentaje()).isEqualByComparingTo("68.1");
        assertThat(Margen.calcular(new BigDecimal("1000"), new BigDecimal("1500")).porcentaje())
                .isEqualByComparingTo("-50.0");
        assertThat(Margen.calcular(BigDecimal.ZERO, new BigDecimal("300")).porcentaje()).isNull();
    }

    @Test
    void loPorCobrarEsElSaldoDelContratoSinQuedarNegativo() {
        ResultadoProyecto parcial = ResultadoProyecto.de(1, "Gala", "Banco", "EN_EJECUCION",
                new BigDecimal("18700"), new BigDecimal("9350"), new BigDecimal("2000"));
        ResultadoProyecto excedido = ResultadoProyecto.de(2, "Feria", "Asociación", "FINALIZADO",
                new BigDecimal("5000"), new BigDecimal("5600"), BigDecimal.ZERO);

        assertThat(parcial.porCobrar()).isEqualByComparingTo("9350");
        assertThat(excedido.porCobrar()).isEqualByComparingTo("0");
    }
}
