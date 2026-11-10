package com.visual.backend.finanzas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.aplicacion.servicio.ParametrosFinanzas;
import com.visual.backend.finanzas.aplicacion.servicio.ServicioIndicadores;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.IndicadoresFinancieros;
import com.visual.backend.finanzas.dominio.modelo.MovimientoDestacado;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.SumasPeriodo;
import com.visual.backend.finanzas.dominio.modelo.TotalMensual;
import com.visual.backend.finanzas.dominio.puerto.ConsultaIndicadores;
import com.visual.backend.finanzas.dominio.puerto.ConsultaResultados;

class ServicioIndicadoresTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay(ZONA).toInstant(), ZONA);
    private static final ParametrosFinanzas PARAMETROS = new ParametrosFinanzas(new BigDecimal("5000"), 30);

    /** Doble de la consulta que registra los periodos pedidos. */
    private static final class ConsultaFalsa implements ConsultaIndicadores {
        private final List<LocalDate[]> periodos = new ArrayList<>();
        private LocalDate[] destacados;

        @Override
        public SumasPeriodo sumas(LocalDate desde, LocalDate hasta) {
            periodos.add(new LocalDate[] {desde, hasta});
            return new SumasPeriodo(new BigDecimal("10000"), new BigDecimal("4000"), new BigDecimal("1500"));
        }

        @Override
        public List<TotalMensual> totalesMensuales(LocalDate desde, LocalDate hasta) {
            return List.of(new TotalMensual(YearMonth.of(2026, 10), new BigDecimal("7000"), new BigDecimal("2000")));
        }

        @Override
        public List<MovimientoDestacado> destacados(LocalDate desde, LocalDate hasta, BigDecimal umbral, int limite) {
            destacados = new LocalDate[] {desde, hasta};
            return List.of();
        }
    }

    private static ConsultaResultados resultados(List<ResultadoProyecto> proyectos) {
        return new ConsultaResultados() {
            @Override
            public Pagina<ResultadoProyecto> buscarProyectos(Filtros.ResultadosProyectos filtro,
                                                              SolicitudPagina solicitud) {
                return new Pagina<>(proyectos, 0, solicitud.tamano(), proyectos.size(), 1);
            }

            @Override
            public Optional<DetalleResultadoProyecto> detalleProyecto(Integer idProyecto) {
                return Optional.empty();
            }
        };
    }

    private static ResultadoProyecto proyecto(int id, String estado, String contratado, String ingresos,
                                              String costos) {
        return ResultadoProyecto.de(id, "Proyecto " + id, "Cliente", estado, new BigDecimal(contratado),
                new BigDecimal(ingresos), new BigDecimal(costos));
    }

    @Test
    void comparaElMesEnCursoConElPeriodoAnteriorDeIgualDuracion() {
        ConsultaFalsa consulta = new ConsultaFalsa();
        IndicadoresFinancieros indicadores = new ServicioIndicadores(consulta, resultados(List.of()), PARAMETROS, RELOJ)
                .calcular(null, null);

        assertThat(consulta.periodos.get(0)).containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));
        assertThat(consulta.periodos.get(1)).containsExactly(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 31));
        assertThat(consulta.destacados).containsExactly(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 10));
        assertThat(indicadores.actual().resultadoNeto()).isEqualByComparingTo("4500");
        assertThat(indicadores.actual().margenBruto().porcentaje()).isEqualByComparingTo("60.0");
        assertThat(indicadores.evolucion()).hasSize(12);
        assertThat(indicadores.evolucion().get(0).mes()).isEqualTo(YearMonth.of(2025, 12));
        assertThat(indicadores.evolucion().get(10).ingresos()).isEqualByComparingTo("7000");
        assertThat(indicadores.evolucion().get(11).ingresos()).isEqualByComparingTo("0");
    }

    @Test
    void ordenaMargenesYCuentasPorCobrarExcluyendoCanceladosYProyectosSinMovimientos() {
        IndicadoresFinancieros indicadores = new ServicioIndicadores(new ConsultaFalsa(), resultados(List.of(
                proyecto(1, "FINALIZADO", "55600", "55600", "47700"),
                proyecto(2, "EN_EJECUCION", "23730", "11865", "6900"),
                proyecto(3, "PLANIFICACION", "4500", "0", "0"),
                proyecto(4, "CANCELADO", "9000", "1000", "0"))), PARAMETROS, RELOJ).calcular(null, null);

        assertThat(indicadores.margenProyectos()).extracting(ResultadoProyecto::idProyecto).containsExactly(1, 2, 4);
        assertThat(indicadores.cuentasPorCobrar()).extracting(ResultadoProyecto::idProyecto).containsExactly(2, 3);
        assertThat(indicadores.totalPorCobrar()).isEqualByComparingTo("16365");
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        ServicioIndicadores servicio = new ServicioIndicadores(new ConsultaFalsa(), resultados(List.of()), PARAMETROS,
                RELOJ);
        assertThatThrownBy(() -> servicio.calcular(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }
}
