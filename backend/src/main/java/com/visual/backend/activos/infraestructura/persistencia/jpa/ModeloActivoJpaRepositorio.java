package com.visual.backend.activos.infraestructura.persistencia.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.activos.dominio.modelo.TipoControl;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModeloActivoEntidad;

public interface ModeloActivoJpaRepositorio extends JpaRepository<ModeloActivoEntidad, Integer> {

    @EntityGraph(attributePaths = {"fabricante", "categoria"})
    List<ModeloActivoEntidad> findAllByOrderByFabricanteNombreAscNombreAsc();

    @EntityGraph(attributePaths = {"fabricante", "categoria"})
    List<ModeloActivoEntidad> findByTipoControlOrderByFabricanteNombreAscNombreAsc(TipoControl tipoControl);

    @EntityGraph(attributePaths = {"fabricante", "categoria"})
    Optional<ModeloActivoEntidad> findWithRelacionesById(Integer id);

    boolean existsByFabricanteIdAndNombreIgnoreCase(Integer idFabricante, String nombre);

    boolean existsByFabricanteIdAndNombreIgnoreCaseAndIdNot(Integer idFabricante, String nombre, Integer id);

    boolean existsByPrefijoCodigo(String prefijoCodigo);

    boolean existsByPrefijoCodigoAndIdNot(String prefijoCodigo, Integer id);
}
