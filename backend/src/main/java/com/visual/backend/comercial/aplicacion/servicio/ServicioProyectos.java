package com.visual.backend.comercial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoProyecto;
import com.visual.backend.comercial.aplicacion.dto.VistaCotizacion;
import com.visual.backend.comercial.aplicacion.dto.VistaDetalleProyecto;
import com.visual.backend.comercial.aplicacion.dto.VistaProyecto;
import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.puerto.RepositorioContratos;
import com.visual.backend.comercial.dominio.puerto.RepositorioCotizaciones;
import com.visual.backend.comercial.dominio.puerto.RepositorioEventos;
import com.visual.backend.comercial.dominio.puerto.RepositorioProyectos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Proyectos generados por cotizaciones aceptadas: datos, periodo y estado. */
@Service
public class ServicioProyectos {

    public static final String ENTIDAD = "proyectos";

    private final RepositorioProyectos repositorio;
    private final RepositorioCotizaciones cotizaciones;
    private final RepositorioEventos eventos;
    private final RepositorioContratos contratos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioProyectos(RepositorioProyectos repositorio, RepositorioCotizaciones cotizaciones,
                             RepositorioEventos eventos, RepositorioContratos contratos, ServicioAuditoria auditoria,
                             ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.cotizaciones = cotizaciones;
        this.eventos = eventos;
        this.contratos = contratos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaProyecto> listar(Filtros.Proyectos filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud).mapear(VistaProyecto::de);
    }

    @Transactional(readOnly = true)
    public VistaDetalleProyecto obtener(Integer id) {
        Proyecto proyecto = repositorio.obtener(id);
        return new VistaDetalleProyecto(VistaProyecto.de(proyecto),
                VistaCotizacion.de(cotizaciones.obtener(proyecto.getIdCotizacion()), proyecto.getId()),
                eventos.listarPorProyecto(id), contratos.listarPorProyecto(id));
    }

    /** El nuevo periodo debe seguir conteniendo los eventos no cancelados del proyecto. */
    @Transactional
    public VistaProyecto actualizar(Integer id, ComandoProyecto comando) {
        Proyecto proyecto = repositorio.obtener(id);
        VistaProyecto anterior = VistaProyecto.de(proyecto);
        if (eventos.existenFueraDePeriodo(id, comando.fechaInicio(), comando.fechaFin())) {
            throw new ReglaNegocioExcepcion("El nuevo periodo deja eventos programados fuera del proyecto");
        }
        proyecto.actualizar(comando.nombre(), comando.descripcion(), comando.fechaInicio(), comando.fechaFin(),
                contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaProyecto actualizado = VistaProyecto.de(repositorio.guardar(proyecto));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    @Transactional
    public VistaProyecto cambiarEstado(Integer id, EstadoProyecto estado) {
        Proyecto proyecto = repositorio.obtener(id);
        VistaProyecto anterior = VistaProyecto.de(proyecto);
        proyecto.cambiarEstado(estado, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaProyecto actualizado = VistaProyecto.de(repositorio.guardar(proyecto));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), "Cambio de estado a " + estado);
        return actualizado;
    }
}
