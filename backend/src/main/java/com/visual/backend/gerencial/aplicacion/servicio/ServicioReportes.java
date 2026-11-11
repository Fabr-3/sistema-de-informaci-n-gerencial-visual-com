package com.visual.backend.gerencial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.TipoReporte;
import com.visual.backend.gerencial.dominio.puerto.ConsultaReportes;
import com.visual.backend.gerencial.dominio.puerto.GeneradorReportes;

/** Generacion de los reportes operativos en PDF; cada emision queda en la auditoria. */
@Service
public class ServicioReportes {

    public static final String ENTIDAD = "reportes";

    private final ServicioKpis kpis;
    private final ServicioAlertasActivos alertas;
    private final ConsultaReportes consulta;
    private final GeneradorReportes generador;
    private final ServicioAuditoria auditoria;
    private final Clock reloj;

    public ServicioReportes(ServicioKpis kpis, ServicioAlertasActivos alertas, ConsultaReportes consulta,
                            GeneradorReportes generador, ServicioAuditoria auditoria, Clock reloj) {
        this.kpis = kpis;
        this.alertas = alertas;
        this.consulta = consulta;
        this.generador = generador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /**
     * Sin fechas: el mes en curso hasta hoy. El estado de activos siempre
     * refleja la situacion actual. No es de solo lectura porque audita la emision.
     */
    @Transactional
    public ReporteGenerado generar(TipoReporte tipo, LocalDate desde, LocalDate hasta) {
        Periodo periodo = kpis.periodo(desde, hasta);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        byte[] contenido = switch (tipo) {
            case RESUMEN_GERENCIAL -> generador.resumenGerencial(kpis.calcular(periodo.desde(), periodo.hasta()),
                    alertas.listar(), ahora);
            case EVENTOS -> generador.eventos(periodo, consulta.eventos(periodo), ahora);
            case ESTADO_ACTIVOS -> generador.estadoActivos(consulta.inventario(), consulta.lotes(),
                    alertas.listar(), ahora);
            case MANTENIMIENTOS -> generador.mantenimientos(periodo, consulta.mantenimientos(periodo), ahora);
            case PERSONAL -> generador.personal(periodo, kpis.utilizacion(periodo), ahora);
        };
        String nombre = tipo == TipoReporte.ESTADO_ACTIVOS ? tipo.archivo() + "_" + ahora.toLocalDate()
                : tipo.archivo() + "_" + periodo.desde() + "_" + periodo.hasta();
        auditoria.registrarOperacion(AccionAuditoria.REPORTE_GENERADO, ENTIDAD, tipo.name(), null,
                Map.of("reporte", tipo.titulo(), "desde", periodo.desde().toString(),
                        "hasta", periodo.hasta().toString()), null);
        return new ReporteGenerado(nombre + ".pdf", contenido);
    }
}
