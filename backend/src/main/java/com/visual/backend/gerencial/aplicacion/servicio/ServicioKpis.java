package com.visual.backend.gerencial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.finanzas.dominio.modelo.SumasPeriodo;
import com.visual.backend.finanzas.dominio.puerto.ConsultaIndicadores;
import com.visual.backend.gerencial.dominio.modelo.IndicadoresPeriodo;
import com.visual.backend.gerencial.dominio.modelo.KpisGerenciales;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.RentabilidadRecurrente;
import com.visual.backend.gerencial.dominio.modelo.ResumenAlertas;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionRecursos;
import com.visual.backend.gerencial.dominio.puerto.ConsultaGerencial;

/**
 * KPIs gerenciales: ganancias, margen de utilidad bruta, margen de beneficio
 * neto, rentabilidad recurrente anual y tasa de utilizacion de recursos. Los
 * importes provienen de los movimientos del modulo financiero.
 */
@Service
public class ServicioKpis {

    private final ConsultaIndicadores finanzas;
    private final ConsultaGerencial consulta;
    private final ServicioAlertasActivos alertas;
    private final ParametrosGerencial parametros;
    private final Clock reloj;

    public ServicioKpis(ConsultaIndicadores finanzas, ConsultaGerencial consulta, ServicioAlertasActivos alertas,
                        ParametrosGerencial parametros, Clock reloj) {
        this.finanzas = finanzas;
        this.consulta = consulta;
        this.alertas = alertas;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    /** Sin fechas: el mes en curso hasta hoy. */
    @Transactional(readOnly = true)
    public KpisGerenciales calcular(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        return new KpisGerenciales(indicadores(periodo), indicadores(periodo.anterior()),
                rentabilidadRecurrente(periodo), utilizacion(periodo), ResumenAlertas.de(alertas.listar()));
    }

    @Transactional(readOnly = true)
    public UtilizacionRecursos utilizacion(Periodo periodo) {
        return UtilizacionRecursos.de(consulta.utilizacionEquipos(periodo), consulta.utilizacionPersonal(periodo));
    }

    public Periodo periodo(LocalDate desde, LocalDate hasta) {
        return Periodo.solicitado(desde, hasta, LocalDate.now(reloj));
    }

    private IndicadoresPeriodo indicadores(Periodo periodo) {
        SumasPeriodo sumas = finanzas.sumas(periodo.desde(), periodo.hasta());
        return IndicadoresPeriodo.calcular(periodo, sumas.ingresos(), sumas.costosDirectos(), sumas.gastosGenerales());
    }

    private RentabilidadRecurrente rentabilidadRecurrente(Periodo periodo) {
        Periodo anual = periodo.anual();
        return RentabilidadRecurrente.calcular(anual, consulta.resultadosPorCliente(anual),
                parametros.proyectosClienteRecurrente());
    }
}
