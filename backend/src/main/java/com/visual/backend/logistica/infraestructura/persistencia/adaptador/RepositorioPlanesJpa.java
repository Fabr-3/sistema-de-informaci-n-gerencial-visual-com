package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.puerto.RepositorioPlanes;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.JornadaEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.PlanLogisticoEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.PlanLogisticoJpaRepositorio;

@Repository
public class RepositorioPlanesJpa implements RepositorioPlanes {

    private final PlanLogisticoJpaRepositorio planes;

    public RepositorioPlanesJpa(PlanLogisticoJpaRepositorio planes) {
        this.planes = planes;
    }

    @Override
    public Optional<PlanLogistico> buscarPorEvento(Integer idEvento) {
        return planes.findByIdEventoAndFechaEliminacionIsNull(idEvento).map(MapeadorLogistica::aDominio);
    }

    @Override
    public PlanLogistico guardar(PlanLogistico plan) {
        PlanLogisticoEntidad entidad = plan.getId() == null ? new PlanLogisticoEntidad()
                : planes.getReferenceById(plan.getId());
        if (plan.getId() == null) {
            entidad.setIdEvento(plan.getIdEvento());
        }
        entidad.setDetallesTransporte(plan.getDetallesTransporte());
        entidad.setCreadoPor(plan.getCreadoPor());
        entidad.setFechaCreacion(plan.getFechaCreacion());
        entidad.setModificadoPor(plan.getModificadoPor());
        entidad.setFechaModificacion(plan.getFechaModificacion());
        sincronizarJornadas(entidad, plan);
        return MapeadorLogistica.aDominio(planes.saveAndFlush(entidad));
    }

    /** Conserva las jornadas por id, actualiza las modificadas, agrega las nuevas y quita las ausentes. */
    private static void sincronizarJornadas(PlanLogisticoEntidad entidad, PlanLogistico plan) {
        Map<Integer, JornadaEntidad> existentes = entidad.getJornadas().stream()
                .collect(Collectors.toMap(JornadaEntidad::getId, Function.identity()));
        List<JornadaEntidad> resultado = new ArrayList<>();
        for (Jornada jornada : plan.getJornadas()) {
            JornadaEntidad fila = jornada.id() == null ? null : existentes.get(jornada.id());
            if (fila == null) {
                fila = new JornadaEntidad();
                fila.setPlan(entidad);
                fila.setCreadoPor(plan.getModificadoPor() == null ? plan.getCreadoPor() : plan.getModificadoPor());
                fila.setFechaCreacion(plan.getFechaModificacion() == null ? plan.getFechaCreacion()
                        : plan.getFechaModificacion());
            } else if (cambio(fila, jornada)) {
                fila.setModificadoPor(plan.getModificadoPor());
                fila.setFechaModificacion(plan.getFechaModificacion());
            }
            fila.setTipo(jornada.tipo());
            fila.setFechaInicio(jornada.periodo().inicio());
            fila.setFechaFin(jornada.periodo().fin());
            fila.setDescripcion(jornada.descripcion());
            resultado.add(fila);
        }
        entidad.getJornadas().retainAll(resultado);
        resultado.stream().filter(fila -> !entidad.getJornadas().contains(fila)).forEach(entidad.getJornadas()::add);
    }

    private static boolean cambio(JornadaEntidad fila, Jornada jornada) {
        return fila.getTipo() != jornada.tipo() || !fila.getFechaInicio().equals(jornada.periodo().inicio())
                || !fila.getFechaFin().equals(jornada.periodo().fin())
                || !Objects.equals(fila.getDescripcion(), jornada.descripcion());
    }
}
