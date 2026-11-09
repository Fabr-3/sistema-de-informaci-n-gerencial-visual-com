package com.visual.backend.activos.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.ModuloLed;

public interface RepositorioModulosLed {

    List<ModuloLed> listarPorActivo(Integer idActivoPadre);

    Optional<ModuloLed> buscarPorId(Integer id);

    boolean existeCodigo(String codigo);

    ModuloLed guardar(ModuloLed modulo, Integer actor);
}
