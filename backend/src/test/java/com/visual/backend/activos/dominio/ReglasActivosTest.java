package com.visual.backend.activos.dominio;

import static com.visual.backend.activos.dominio.FabricaPruebasActivos.AHORA;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.BODEGA;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.modelo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.CondicionFinal;
import com.visual.backend.activos.dominio.modelo.Especificacion;
import com.visual.backend.activos.dominio.modelo.ExistenciaLote;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.NivelAlertaMantenimiento;
import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.activos.dominio.modelo.TipoControl;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;
import com.visual.backend.activos.dominio.modelo.TipoMovimientoLote;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class ReglasActivosTest {

    @ParameterizedTest
    @CsvSource({"0, AL_DIA", "89.99, AL_DIA", "90, PROXIMO", "99.5, PROXIMO", "100, VENCIDO", "250, VENCIDO"})
    void calculaElNivelDeAlertaPorHoras(BigDecimal horas, NivelAlertaMantenimiento esperado) {
        assertThat(NivelAlertaMantenimiento.calcular(horas, 100, 90)).isEqualTo(esperado);
    }

    @Test
    void sinIntervaloNoHayPlanDeMantenimiento() {
        assertThat(NivelAlertaMantenimiento.calcular(BigDecimal.valueOf(9999), null, 90))
                .isEqualTo(NivelAlertaMantenimiento.SIN_PLAN);
    }

    @Test
    void losModelosPorLoteNoTienenIntervaloDeHorasNiEspecificacionesRepetidas() {
        assertThatThrownBy(() -> modelo(TipoControl.LOTE, 100)).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> new ModeloActivo(null, "X", new Fabricante(1, "F"), new Categoria(1, "C"),
                TipoControl.UNIDAD, null, null,
                List.of(new Especificacion("Pitch", "3.9"), new Especificacion("pitch ", "2.6"))))
                .hasMessageContaining("repetida");
    }

    @Test
    void elPrefijoGeneraCodigosCorrelativosYSoloAplicaAModelosPorUnidad() {
        ModeloActivo modelo = new ModeloActivo(1, "VX1000", new Fabricante(1, "NovaStar"), new Categoria(2, "Video"),
                TipoControl.UNIDAD, null, " nova ", List.of());

        assertThat(modelo.prefijoCodigo()).isEqualTo("NOVA");
        assertThat(modelo.codigoInventario(7)).isEqualTo("NOVA-0007");
        assertThatThrownBy(() -> new ModeloActivo(1, "X", new Fabricante(1, "F"), new Categoria(1, "C"),
                TipoControl.UNIDAD, null, "NO VA", List.of())).hasMessageContaining("prefijo");
        assertThatThrownBy(() -> new ModeloActivo(1, "X", new Fabricante(1, "F"), new Categoria(1, "C"),
                TipoControl.LOTE, null, "CAB", List.of())).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> new ModeloActivo(1, "X", new Fabricante(1, "F"), new Categoria(1, "C"),
                TipoControl.UNIDAD, null, null, List.of()).codigoInventario(1)).hasMessageContaining("prefijo");
    }

    @Test
    void elUsoDebeSerUnPeriodoPasadoValido() {
        RegistroUso uso = RegistroUso.nuevo(1, AHORA.minusHours(3), AHORA.minusMinutes(30), " Feria ", 1, AHORA);
        assertThat(uso.horas()).isEqualByComparingTo("2.50");
        assertThat(uso.descripcion()).isEqualTo("Feria");

        assertThatThrownBy(() -> RegistroUso.nuevo(1, AHORA, AHORA.minusHours(1), null, 1, AHORA))
                .hasMessageContaining("posterior");
        assertThatThrownBy(() -> RegistroUso.nuevo(1, AHORA.minusHours(1), AHORA.plusHours(1), null, 1, AHORA))
                .hasMessageContaining("futura");
    }

    @Test
    void lasExistenciasPorLoteNuncaQuedanNegativas() {
        ExistenciaLote existencia = ExistenciaLote.nueva(modelo(TipoControl.LOTE, null), BODEGA);

        assertThat(existencia.aplicar(TipoMovimientoLote.INGRESO, 10)).isEqualTo(10);
        assertThat(existencia.aplicar(TipoMovimientoLote.SALIDA, 4)).isEqualTo(-4);
        assertThat(existencia.aplicar(TipoMovimientoLote.AJUSTE, -1)).isEqualTo(-1);
        assertThat(existencia.getCantidad()).isEqualTo(5);

        assertThatThrownBy(() -> existencia.aplicar(TipoMovimientoLote.BAJA, 6)).hasMessageContaining("insuficiente");
        assertThatThrownBy(() -> existencia.aplicar(TipoMovimientoLote.SALIDA, -2)).hasMessageContaining("positiva");
        assertThatThrownBy(() -> existencia.aplicar(TipoMovimientoLote.INGRESO, 0)).hasMessageContaining("cero");
        assertThat(existencia.getCantidad()).isEqualTo(5);
    }

    @Test
    void unaBodegaInactivaSoloAdmiteSalidas() {
        ExistenciaLote existencia = new ExistenciaLote(1, modelo(TipoControl.LOTE, null),
                FabricaPruebasActivos.BODEGA_INACTIVA, 5);

        assertThat(existencia.aplicar(TipoMovimientoLote.SALIDA, 2)).isEqualTo(-2);
        assertThatThrownBy(() -> existencia.aplicar(TipoMovimientoLote.INGRESO, 1)).hasMessageContaining("inactiva");
    }

    @Test
    void soloLosModelosPorLoteTienenExistencias() {
        assertThatThrownBy(() -> ExistenciaLote.nueva(modelo(TipoControl.UNIDAD, null), BODEGA))
                .hasMessageContaining("por unidad");
    }

    @Test
    void elMantenimientoSeFinalizaUnaSolaVezConFechaValida() {
        Mantenimiento mantenimiento = Mantenimiento.iniciar(1, new TipoMantenimiento(1, "Preventivo", true),
                AHORA.minusDays(1), null, 1, AHORA);

        assertThatThrownBy(() -> mantenimiento.finalizar(AHORA.minusDays(2), null, null, null, 1, AHORA))
                .hasMessageContaining("fecha de finalización");

        mantenimiento.finalizar(AHORA, CondicionFinal.OPERATIVO,
                "Limpieza", BigDecimal.ONE, 1, AHORA);
        assertThat(mantenimiento.estaEnProceso()).isFalse();
        assertThatThrownBy(() -> mantenimiento.finalizar(AHORA, null, null, null, 1, AHORA))
                .hasMessageContaining("ya fue finalizado");
    }
}
