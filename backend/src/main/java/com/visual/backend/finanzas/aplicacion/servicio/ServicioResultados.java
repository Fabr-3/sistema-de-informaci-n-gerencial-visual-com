package com.visual.backend.finanzas.aplicacion.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.puerto.ConsultaResultados;

/** Costos, ingresos y margen de ganancia por proyecto y por evento. */
@Service
public class ServicioResultados {

    private final ConsultaResultados consulta;

    public ServicioResultados(ConsultaResultados consulta) {
        this.consulta = consulta;
    }

    @Transactional(readOnly = true)
    public Pagina<ResultadoProyecto> listarProyectos(Filtros.ResultadosProyectos filtro, SolicitudPagina solicitud) {
        return consulta.buscarProyectos(filtro, solicitud);
    }

    @Transactional(readOnly = true)
    public DetalleResultadoProyecto detalleProyecto(Integer idProyecto) {
        return consulta.obtenerDetalle(idProyecto);
    }
}
