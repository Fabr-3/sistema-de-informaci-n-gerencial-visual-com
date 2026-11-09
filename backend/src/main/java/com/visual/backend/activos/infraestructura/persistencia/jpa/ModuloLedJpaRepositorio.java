package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.ModuloLedEntidad;

public interface ModuloLedJpaRepositorio extends JpaRepository<ModuloLedEntidad, Integer> {

    List<ModuloLedEntidad> findByIdActivoPadreAndFechaEliminacionIsNullOrderByCodigoAsc(Integer idActivoPadre);

    Optional<ModuloLedEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    boolean existsByCodigo(String codigo);

    boolean existsByIdActivoPadre(Integer idActivoPadre);
}
