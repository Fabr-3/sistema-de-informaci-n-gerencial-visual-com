package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.activos.dominio.puerto.RepositorioMantenimientos;
import com.visual.backend.activos.infraestructura.persistencia.entidad.MantenimientoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.MantenimientoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.TipoMantenimientoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

@Repository
public class RepositorioMantenimientosJpa implements RepositorioMantenimientos {

    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaInicio"));

    private final MantenimientoJpaRepositorio mantenimientos;
    private final TipoMantenimientoJpaRepositorio tipos;

    public RepositorioMantenimientosJpa(MantenimientoJpaRepositorio mantenimientos,
                                        TipoMantenimientoJpaRepositorio tipos) {
        this.mantenimientos = mantenimientos;
        this.tipos = tipos;
    }

    @Override
    public Pagina<Mantenimiento> listarPorActivo(Integer idActivo, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(mantenimientos.findByIdActivoAndFechaEliminacionIsNull(idActivo,
                ConsultasJpa.paginacion(solicitud, RECIENTES_PRIMERO)).map(MapeadorActivos::aDominio));
    }

    @Override
    public Optional<Mantenimiento> buscarPorId(Integer id) {
        return mantenimientos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public Mantenimiento guardar(Mantenimiento mantenimiento) {
        MantenimientoEntidad entidad = mantenimiento.getId() == null ? new MantenimientoEntidad()
                : mantenimientos.getReferenceById(mantenimiento.getId());
        entidad.setIdActivo(mantenimiento.getIdActivo());
        entidad.setTipo(tipos.getReferenceById(mantenimiento.getTipo().id()));
        entidad.setFechaInicio(mantenimiento.getFechaInicio());
        entidad.setFechaFin(mantenimiento.getFechaFin());
        entidad.setDescripcion(mantenimiento.getDescripcion());
        entidad.setObservaciones(mantenimiento.getObservaciones());
        entidad.setCondicionFinal(mantenimiento.getCondicionFinal());
        entidad.setCosto(mantenimiento.getCosto());
        entidad.setEstado(mantenimiento.getEstado());
        entidad.setCreadoPor(mantenimiento.getCreadoPor());
        entidad.setModificadoPor(mantenimiento.getModificadoPor());
        entidad.setFechaCreacion(mantenimiento.getFechaCreacion());
        entidad.setFechaModificacion(mantenimiento.getFechaModificacion());
        return MapeadorActivos.aDominio(mantenimientos.save(entidad));
    }
}
