package com.visual.backend.finanzas.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.finanzas.aplicacion.dto.ArchivoPdf;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.EstadoMovimiento;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.puerto.ConsultaResultados;
import com.visual.backend.finanzas.dominio.puerto.GeneradorPdfFinanzas;
import com.visual.backend.finanzas.dominio.puerto.RepositorioMovimientos;

/** Costos, ingresos y margen de ganancia por proyecto y por evento. */
@Service
public class ServicioResultados {

    private final ConsultaResultados consulta;
    private final RepositorioMovimientos movimientos;
    private final GeneradorPdfFinanzas generador;
    private final Clock reloj;

    public ServicioResultados(ConsultaResultados consulta, RepositorioMovimientos movimientos,
                              GeneradorPdfFinanzas generador, Clock reloj) {
        this.consulta = consulta;
        this.movimientos = movimientos;
        this.generador = generador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<ResultadoProyecto> listarProyectos(Filtros.ResultadosProyectos filtro, SolicitudPagina solicitud) {
        return consulta.buscarProyectos(filtro, solicitud);
    }

    @Transactional(readOnly = true)
    public DetalleResultadoProyecto detalleProyecto(Integer idProyecto) {
        return consulta.obtenerDetalle(idProyecto);
    }

    /** PDF con los movimientos registrados del proyecto (sin anulados), por fecha, y sus resultados. */
    @Transactional(readOnly = true)
    public ArchivoPdf pdfMovimientosProyecto(Integer idProyecto) {
        DetalleResultadoProyecto detalle = consulta.obtenerDetalle(idProyecto);
        List<MovimientoFinanciero> registrados = movimientosRegistrados(idProyecto).stream()
                .sorted(Comparator.comparing(MovimientoFinanciero::getFecha)
                        .thenComparing(MovimientoFinanciero::getId))
                .toList();
        byte[] contenido = generador.movimientosProyecto(detalle, registrados, LocalDateTime.now(reloj));
        return new ArchivoPdf("movimientos-proyecto-" + idProyecto + ".pdf", contenido);
    }

    private List<MovimientoFinanciero> movimientosRegistrados(Integer idProyecto) {
        Filtros.Movimientos filtro = new Filtros.Movimientos(null, null, null, null, idProyecto, null, null,
                EstadoMovimiento.REGISTRADO, null);
        List<MovimientoFinanciero> todos = new ArrayList<>();
        int pagina = 0;
        Pagina<MovimientoFinanciero> actual;
        do {
            actual = movimientos.buscar(filtro, new SolicitudPagina(pagina++, SolicitudPagina.TAMANO_MAXIMO));
            todos.addAll(actual.contenido());
        } while (todos.size() < actual.totalElementos() && !actual.contenido().isEmpty());
        return todos;
    }
}
