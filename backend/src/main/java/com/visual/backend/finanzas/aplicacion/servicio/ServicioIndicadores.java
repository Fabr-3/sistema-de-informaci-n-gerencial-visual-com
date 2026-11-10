package com.visual.backend.finanzas.aplicacion.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.IndicadoresFinancieros;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.ResumenPeriodo;
import com.visual.backend.finanzas.dominio.modelo.SumasPeriodo;
import com.visual.backend.finanzas.dominio.modelo.TotalMensual;
import com.visual.backend.finanzas.dominio.puerto.ConsultaIndicadores;
import com.visual.backend.finanzas.dominio.puerto.ConsultaResultados;

/** Indicadores financieros del gerente para un periodo (por defecto, el mes en curso). */
@Service
public class ServicioIndicadores {

    static final int MESES_EVOLUCION = 12;
    static final int LIMITE_DESTACADOS = 8;
    /** Los proyectos se evaluan completos; la pagina maxima cubre la cartera de la empresa. */
    private static final SolicitudPagina TODOS_LOS_PROYECTOS = new SolicitudPagina(0, SolicitudPagina.TAMANO_MAXIMO);

    private final ConsultaIndicadores consulta;
    private final ConsultaResultados resultados;
    private final ParametrosFinanzas parametros;
    private final Clock reloj;

    public ServicioIndicadores(ConsultaIndicadores consulta, ConsultaResultados resultados,
                               ParametrosFinanzas parametros, Clock reloj) {
        this.consulta = consulta;
        this.resultados = resultados;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public IndicadoresFinancieros calcular(LocalDate desdeSolicitado, LocalDate hastaSolicitado) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate hasta = hastaSolicitado == null ? hoy : hastaSolicitado;
        LocalDate desde = desdeSolicitado == null ? hasta.withDayOfMonth(1) : desdeSolicitado;
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioExcepcion("La fecha inicial no puede ser posterior a la final");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = hastaAnterior.minusDays(dias - 1);

        List<ResultadoProyecto> proyectos = resultados.buscarProyectos(new Filtros.ResultadosProyectos(null, null),
                TODOS_LOS_PROYECTOS).contenido();
        List<ResultadoProyecto> porCobrar = cuentasPorCobrar(proyectos);
        return new IndicadoresFinancieros(resumen(desde, hasta), resumen(desdeAnterior, hastaAnterior),
                evolucion(YearMonth.from(hasta)), margenProyectos(proyectos), porCobrar,
                porCobrar.stream().map(ResultadoProyecto::porCobrar).reduce(BigDecimal.ZERO, BigDecimal::add),
                consulta.destacados(hasta.minusDays(parametros.diasRecientes() - 1L), hasta,
                        parametros.umbralSignificativo(), LIMITE_DESTACADOS),
                parametros.umbralSignificativo());
    }

    private ResumenPeriodo resumen(LocalDate desde, LocalDate hasta) {
        SumasPeriodo sumas = consulta.sumas(desde, hasta);
        return ResumenPeriodo.de(desde, hasta, sumas.ingresos(), sumas.costosDirectos(), sumas.gastosGenerales());
    }

    /** Los ultimos meses hasta el mes indicado, incluidos los que no tuvieron movimientos. */
    private List<TotalMensual> evolucion(YearMonth ultimo) {
        YearMonth primero = ultimo.minusMonths(MESES_EVOLUCION - 1L);
        Map<YearMonth, TotalMensual> conMovimientos = consulta.totalesMensuales(primero.atDay(1), ultimo.atEndOfMonth())
                .stream().collect(Collectors.toMap(TotalMensual::mes, Function.identity()));
        List<TotalMensual> meses = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            meses.add(conMovimientos.getOrDefault(mes, TotalMensual.vacio(mes)));
        }
        return meses;
    }

    /** Proyectos con movimientos, del mayor al menor margen. */
    private static List<ResultadoProyecto> margenProyectos(List<ResultadoProyecto> proyectos) {
        return proyectos.stream()
                .filter(proyecto -> proyecto.margen().ingresos().signum() != 0
                        || proyecto.margen().costos().signum() != 0)
                .sorted(Comparator.comparing((ResultadoProyecto proyecto) -> proyecto.margen().margen()).reversed())
                .toList();
    }

    /** Saldo por cobrar de los proyectos no cancelados, del mayor al menor. */
    private static List<ResultadoProyecto> cuentasPorCobrar(List<ResultadoProyecto> proyectos) {
        return proyectos.stream()
                .filter(proyecto -> proyecto.porCobrar().signum() > 0
                        && !EstadoProyecto.CANCELADO.name().equals(proyecto.estado()))
                .sorted(Comparator.comparing(ResultadoProyecto::porCobrar).reversed())
                .toList();
    }
}
