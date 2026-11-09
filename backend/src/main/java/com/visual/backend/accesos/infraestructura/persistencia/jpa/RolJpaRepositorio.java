package com.visual.backend.accesos.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.RolEntidad;

public interface RolJpaRepositorio extends JpaRepository<RolEntidad, Integer> {

    @EntityGraph(attributePaths = "permisos")
    List<RolEntidad> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "permisos")
    Optional<RolEntidad> findWithPermisosById(Integer id);

    @EntityGraph(attributePaths = "permisos")
    Optional<RolEntidad> findByCodigo(CodigoRol codigo);
}
