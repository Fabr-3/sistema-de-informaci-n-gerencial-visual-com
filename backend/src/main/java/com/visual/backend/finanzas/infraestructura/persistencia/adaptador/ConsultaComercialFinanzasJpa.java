package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.finanzas.dominio.modelo.Referencia;
import com.visual.backend.finanzas.dominio.puerto.ConsultaComercialFinanzas;

import jakarta.persistence.EntityManager;

@Repository
public class ConsultaComercialFinanzasJpa implements ConsultaComercialFinanzas {

    private final EntityManager entityManager;

    public ConsultaComercialFinanzasJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Referencia> buscarProyecto(Integer idProyecto) {
        return Optional.ofNullable(entityManager.find(ProyectoEntidad.class, idProyecto))
                .filter(proyecto -> proyecto.getFechaEliminacion() == null)
                .map(proyecto -> new Referencia(proyecto.getId(), proyecto.getNombre()));
    }

    @Override
    public Optional<Referencia> buscarEventoDelProyecto(Integer idEvento, Integer idProyecto) {
        return Optional.ofNullable(entityManager.find(EventoEntidad.class, idEvento))
                .filter(evento -> evento.getFechaEliminacion() == null
                        && evento.getProyecto().getId().equals(idProyecto))
                .map(evento -> new Referencia(evento.getId(), evento.getNombre()));
    }
}
