package com.visual.backend.gerencial.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.gerencial.dominio.modelo.ActivoAfectado;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.IndicadoresPeriodo;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.RentabilidadRecurrente;
import com.visual.backend.gerencial.dominio.modelo.ResultadoCliente;
import com.visual.backend.gerencial.dominio.modelo.ResumenAlertas;
import com.visual.backend.gerencial.dominio.modelo.Severidad;
import com.visual.backend.gerencial.dominio.modelo.TasaUtilizacion;
import com.visual.backend.gerencial.dominio.modelo.TipoAlertaActivo;

class ReglasGerencialTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final ActivoAfectado PANTALLA = new ActivoAfectado(5, "ABPL-0003", "Absen PL3.9 Pro V2",
            "Pantalla LED");
    private static final int MINIMO_RECURRENTE = 2;

    @Test
    void elPeriodoPorDefectoEsElMesEnCursoYSeComparaConElAnterior() {
        Periodo mes = Periodo.solicitado(null, null, HOY);
        assertThat(mes).isEqualTo(new Periodo(LocalDate.of(2026, 11, 1), HOY));
        assertThat(mes.dias()).isEqualTo(10);
        assertThat(mes.anterior()).isEqualTo(new Periodo(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 31)));
        assertThat(mes.anual()).isEqualTo(new Periodo(LocalDate.of(2025, 11, 11), HOY));
        assertThatThrownBy(() -> Periodo.solicitado(HOY, HOY.minusDays(1), HOY))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void calculaGananciaYMargenesBrutoYNeto() {
        IndicadoresPeriodo indicadores = IndicadoresPeriodo.calcular(new Periodo(HOY, HOY), monto("10000"),
                monto("4000"), monto("1500"));
        assertThat(indicadores.utilidadBruta()).isEqualByComparingTo("6000");
        assertThat(indicadores.ganancia()).isEqualByComparingTo("4500");
        assertThat(indicadores.margenBruto()).isEqualByComparingTo("60.0");
        assertThat(indicadores.margenNeto()).isEqualByComparingTo("45.0");

        IndicadoresPeriodo sinIngresos = IndicadoresPeriodo.calcular(new Periodo(HOY, HOY), BigDecimal.ZERO,
                BigDecimal.ZERO, monto("800"));
        assertThat(sinIngresos.ganancia()).isEqualByComparingTo("-800");
        assertThat(sinIngresos.margenBruto()).isNull();
        assertThat(sinIngresos.margenNeto()).isNull();
    }

    @Test
    void laRentabilidadRecurrenteConsideraSoloClientesConVariosProyectos() {
        ResultadoCliente banco = ResultadoCliente.de(1, "Banco Andino", 3, 2, monto("20000"), monto("5000"));
        ResultadoCliente cerveceria = ResultadoCliente.de(2, "Cervecería del Altiplano", 2, 1, monto("10000"),
                monto("8000"));
        ResultadoCliente nuevo = ResultadoCliente.de(3, "Cliente nuevo", 1, 1, monto("10000"), monto("7000"));

        RentabilidadRecurrente rentabilidad = RentabilidadRecurrente.calcular(Periodo.solicitado(null, null, HOY)
                .anual(), List.of(nuevo, cerveceria, banco), MINIMO_RECURRENTE);

        assertThat(rentabilidad.clientesRecurrentes()).isEqualTo(2);
        assertThat(rentabilidad.clientesConProyectos()).isEqualTo(3);
        assertThat(rentabilidad.margen()).isEqualByComparingTo("17000");
        assertThat(rentabilidad.porcentaje()).isEqualByComparingTo("56.7");
        assertThat(rentabilidad.margenTotalProyectos()).isEqualByComparingTo("20000");
        assertThat(rentabilidad.participacion()).isEqualByComparingTo("85.0");
        assertThat(rentabilidad.clientes()).extracting(ResultadoCliente::cliente)
                .containsExactly("Banco Andino", "Cervecería del Altiplano");

        RentabilidadRecurrente conPerdida = RentabilidadRecurrente.calcular(new Periodo(HOY, HOY),
                List.of(ResultadoCliente.de(4, "Con pérdida", 2, 1, monto("1000"), monto("3000"))),
                MINIMO_RECURRENTE);
        assertThat(conPerdida.porcentaje()).isEqualByComparingTo("-200.0");
        assertThat(conPerdida.participacion()).isNull();
    }

    @Test
    void laTasaDeUtilizacionSumaDiasOcupadosSobreDisponibles() {
        TasaUtilizacion total = TasaUtilizacion.total(List.of(TasaUtilizacion.de(2, 60, 15),
                TasaUtilizacion.de(1, 30, 0)));
        assertThat(total.recursos()).isEqualTo(3);
        assertThat(total.porcentaje()).isEqualByComparingTo("16.7");
        assertThat(TasaUtilizacion.de(0, 0, 0).porcentaje()).isNull();
    }

    @Test
    void lasAlertasDescribenLaSituacionYSeOrdenanPorUrgencia() {
        AlertaActivo vencido = AlertaActivo.mantenimientoPreventivo(PANTALLA, true, new BigDecimal("125.25"), 120);
        AlertaActivo sinUso = AlertaActivo.sinUso(PANTALLA, null, HOY);
        AlertaActivo prolongado = AlertaActivo.mantenimientoProlongado(PANTALLA, HOY.minusDays(9), HOY);
        AlertaActivo defectuosos = AlertaActivo.modulosDefectuosos(PANTALLA, 2);

        assertThat(vencido.tipo()).isEqualTo(TipoAlertaActivo.MANTENIMIENTO_VENCIDO);
        assertThat(vencido.severidad()).isEqualTo(Severidad.CRITICA);
        assertThat(vencido.detalle()).startsWith("125,3 h de uso");
        assertThat(prolongado.detalle()).isEqualTo("En mantenimiento desde el 01/11/2026 (9 días)");
        assertThat(defectuosos.detalle()).isEqualTo("2 módulos LED defectuosos");
        assertThat(sinUso.detalle()).isEqualTo("Nunca registró uso");
        assertThat(AlertaActivo.sinUso(PANTALLA, HOY.minusDays(1), HOY).detalle()).endsWith("(hace 1 día)");

        List<AlertaActivo> ordenadas = List.of(sinUso, defectuosos, vencido, prolongado).stream()
                .sorted(AlertaActivo.POR_URGENCIA).toList();
        assertThat(ordenadas).containsExactly(vencido, prolongado, defectuosos, sinUso);
        assertThat(ResumenAlertas.de(ordenadas)).isEqualTo(new ResumenAlertas(1, 2, 1));
    }

    private static BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }
}
