package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.activos.dominio.puerto.RepositorioRegistrosUso;
import com.visual.backend.activos.infraestructura.persistencia.entidad.RegistroUsoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.RegistroUsoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

@Repository
public class RepositorioRegistrosUsoJpa implements RepositorioRegistrosUso {

    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaInicio"));

    private final RegistroUsoJpaRepositorio registros;

    public RepositorioRegistrosUsoJpa(RegistroUsoJpaRepositorio registros) {
        this.registros = registros;
    }

    @Override
    public Pagina<RegistroUso> listarPorActivo(Integer idActivo, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(registros.findByIdActivoAndFechaEliminacionIsNull(idActivo,
                ConsultasJpa.paginacion(solicitud, RECIENTES_PRIMERO)).map(MapeadorActivos::aDominio));
    }

    @Override
    public Optional<RegistroUso> buscarPorId(Integer id) {
        return registros.findByIdAndFechaEliminacionIsNull(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeSuperposicion(Integer idActivo, LocalDateTime inicio, LocalDateTime fin) {
        return registros.existeSuperposicion(idActivo, inicio, fin);
    }

    @Override
    public RegistroUso guardar(RegistroUso registro) {
        RegistroUsoEntidad entidad = new RegistroUsoEntidad();
        entidad.setIdActivo(registro.idActivo());
        entidad.setFechaInicio(registro.fechaInicio());
        entidad.setFechaFin(registro.fechaFin());
        entidad.setDescripcion(registro.descripcion());
        entidad.setCreadoPor(registro.creadoPor());
        entidad.setFechaCreacion(registro.fechaCreacion());
        return MapeadorActivos.aDominio(registros.save(entidad));
    }

    @Override
    public void anular(Integer id, Integer actor, LocalDateTime ahora) {
        RegistroUsoEntidad entidad = registros.getReferenceById(id);
        entidad.setEliminadoPor(actor);
        entidad.setFechaEliminacion(ahora);
        registros.save(entidad);
    }
}
