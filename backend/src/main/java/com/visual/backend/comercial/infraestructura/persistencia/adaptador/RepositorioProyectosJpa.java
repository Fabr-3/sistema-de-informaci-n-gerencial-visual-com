package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.puerto.RepositorioProyectos;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.CotizacionJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ProyectoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

import jakarta.persistence.criteria.Predicate;

@Repository
public class RepositorioProyectosJpa implements RepositorioProyectos {

    /** Los proyectos sin fecha se listan al final. */
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Order.desc("fechaInicio").nullsLast(),
            Sort.Order.desc("id"));

    private final ProyectoJpaRepositorio proyectos;
    private final CotizacionJpaRepositorio cotizaciones;

    public RepositorioProyectosJpa(ProyectoJpaRepositorio proyectos, CotizacionJpaRepositorio cotizaciones) {
        this.proyectos = proyectos;
        this.cotizaciones = cotizaciones;
    }

    @Override
    public Optional<Proyecto> buscarPorId(Integer id) {
        return proyectos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public Optional<Proyecto> buscarPorCotizacion(Integer idCotizacion) {
        return proyectos.findByCotizacionIdAndFechaEliminacionIsNull(idCotizacion).map(MapeadorComercial::aDominio);
    }

    @Override
    public Map<Integer, Integer> idsPorCotizacion(Collection<Integer> idsCotizaciones) {
        if (idsCotizaciones.isEmpty()) {
            return Map.of();
        }
        return proyectos.buscarIdsPorCotizacion(idsCotizaciones).stream()
                .collect(Collectors.toMap(ProyectoJpaRepositorio.ProyectoPorCotizacion::getIdCotizacion,
                        ProyectoJpaRepositorio.ProyectoPorCotizacion::getIdProyecto));
    }

    @Override
    public Pagina<Proyecto> buscar(Filtros.Proyectos filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(proyectos.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorComercial::aDominio));
    }

    @Override
    public Proyecto guardar(Proyecto proyecto) {
        ProyectoEntidad entidad = proyecto.getId() == null ? new ProyectoEntidad()
                : proyectos.getReferenceById(proyecto.getId());
        if (proyecto.getId() == null) {
            entidad.setCotizacion(cotizaciones.getReferenceById(proyecto.getIdCotizacion()));
        }
        entidad.setNombre(proyecto.getNombre());
        entidad.setDescripcion(proyecto.getDescripcion());
        entidad.setFechaInicio(proyecto.getFechaInicio());
        entidad.setFechaFin(proyecto.getFechaFin());
        entidad.setEstado(proyecto.getEstado());
        entidad.setCreadoPor(proyecto.getCreadoPor());
        entidad.setFechaCreacion(proyecto.getFechaCreacion());
        entidad.setModificadoPor(proyecto.getModificadoPor());
        entidad.setFechaModificacion(proyecto.getFechaModificacion());
        return MapeadorComercial.aDominio(proyectos.save(entidad));
    }

    private static Specification<ProyectoEntidad> especificacion(Filtros.Proyectos filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            var cotizacion = raiz.get("cotizacion");
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("nombre")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(cotizacion.get("numero")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(cotizacion.get("cliente").get("razonSocial")), patron,
                                ConsultasJpa.ESCAPE_LIKE)));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            if (filtro.idCliente() != null) {
                condiciones.add(cb.equal(cotizacion.get("cliente").get("id"), filtro.idCliente()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
