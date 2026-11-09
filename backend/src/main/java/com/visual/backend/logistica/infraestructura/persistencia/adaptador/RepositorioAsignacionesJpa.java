package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.AsignacionPropia;
import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.puerto.RepositorioAsignaciones;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.AsignacionEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.AsignacionJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.PersonalTecnicoJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.RolEventoJpaRepositorio;

@Repository
public class RepositorioAsignacionesJpa implements RepositorioAsignaciones {

    private static final int COLUMNA_ASIGNACION = 0;
    private static final int COLUMNA_TRANSPORTE = 1;
    private static final int COLUMNA_EVENTO = 2;

    private final AsignacionJpaRepositorio asignaciones;
    private final PersonalTecnicoJpaRepositorio personal;
    private final RolEventoJpaRepositorio roles;

    public RepositorioAsignacionesJpa(AsignacionJpaRepositorio asignaciones, PersonalTecnicoJpaRepositorio personal,
                                      RolEventoJpaRepositorio roles) {
        this.asignaciones = asignaciones;
        this.personal = personal;
        this.roles = roles;
    }

    @Override
    public Optional<Asignacion> buscarPorId(Integer id) {
        return asignaciones.findById(id).map(MapeadorLogistica::aDominio);
    }

    @Override
    public List<Asignacion> listarPorPlan(Integer idPlan) {
        return asignaciones.findByIdPlanOrderByFechaInicioAscIdAsc(idPlan).stream()
                .map(MapeadorLogistica::aDominio).toList();
    }

    @Override
    public boolean existeEnPlan(Integer idPlan, Integer idPersonal, Periodo periodo, Integer idExcluido) {
        return asignaciones.existeEnPlan(idPlan, idPersonal, periodo.inicio(), periodo.fin(), idExcluido);
    }

    @Override
    public boolean existenFueraDe(Integer idPlan, Periodo periodo) {
        return asignaciones.existenFueraDe(idPlan, periodo.inicio(), periodo.fin());
    }

    @Override
    public List<Cruce> cruces(Integer idPersonal, Periodo periodo, Integer idPlanExcluido) {
        return asignaciones.cruces(idPersonal, periodo.inicio(), periodo.fin(), idPlanExcluido,
                EstadoEvento.CANCELADO);
    }

    @Override
    public List<AsignacionPropia> listarDePersonal(Integer idPersonal, LocalDateTime desde) {
        return asignaciones.listarDePersonal(idPersonal, desde, EstadoEvento.CANCELADO).stream()
                .map(fila -> {
                    AsignacionEntidad asignacion = (AsignacionEntidad) fila[COLUMNA_ASIGNACION];
                    return new AsignacionPropia(asignacion.getId(), asignacion.getIdPlan(),
                            asignacion.getPersonal().getId(),
                            MapeadorLogistica.evento((EventoEntidad) fila[COLUMNA_EVENTO]),
                            asignacion.getRol().getNombre(), asignacion.getFechaInicio(), asignacion.getFechaFin(),
                            (String) fila[COLUMNA_TRANSPORTE]);
                })
                .toList();
    }

    @Override
    public Asignacion guardar(Asignacion asignacion) {
        AsignacionEntidad entidad = asignacion.getId() == null ? new AsignacionEntidad()
                : asignaciones.getReferenceById(asignacion.getId());
        if (asignacion.getId() == null) {
            entidad.setIdPlan(asignacion.getIdPlan());
        }
        entidad.setPersonal(personal.getReferenceById(asignacion.getPersonal().id()));
        entidad.setRol(roles.getReferenceById(asignacion.getRol().id()));
        entidad.setFechaInicio(asignacion.getPeriodo().inicio());
        entidad.setFechaFin(asignacion.getPeriodo().fin());
        entidad.setCreadoPor(asignacion.getCreadoPor());
        entidad.setFechaCreacion(asignacion.getFechaCreacion());
        entidad.setModificadoPor(asignacion.getModificadoPor());
        entidad.setFechaModificacion(asignacion.getFechaModificacion());
        return MapeadorLogistica.aDominio(asignaciones.saveAndFlush(entidad));
    }

    @Override
    public void eliminar(Integer id) {
        asignaciones.deleteById(id);
    }
}
