package com.visual.backend.logistica.dominio.puerto;

import java.util.Optional;

import com.visual.backend.logistica.dominio.modelo.PlanLogistico;

public interface RepositorioPlanes {

    Optional<PlanLogistico> buscarPorEvento(Integer idEvento);

    PlanLogistico guardar(PlanLogistico plan);
}
