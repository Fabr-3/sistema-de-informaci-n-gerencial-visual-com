package com.visual.backend.activos.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;

/** Catalogos simples del modulo: categorias, fabricantes, bodegas y tipos de mantenimiento. */
public interface RepositorioCatalogosActivos {

    List<Categoria> listarCategorias();

    Optional<Categoria> buscarCategoria(Integer id);

    boolean existeCategoria(String nombre, Integer idExcluido);

    Categoria guardarCategoria(Categoria categoria);

    List<Fabricante> listarFabricantes();

    Optional<Fabricante> buscarFabricante(Integer id);

    boolean existeFabricante(String nombre, Integer idExcluido);

    Fabricante guardarFabricante(Fabricante fabricante);

    List<Bodega> listarBodegas();

    Optional<Bodega> buscarBodega(Integer id);

    boolean existeBodega(String nombre, Integer idExcluido);

    Bodega guardarBodega(Bodega bodega, Integer actor);

    List<TipoMantenimiento> listarTiposMantenimiento();

    Optional<TipoMantenimiento> buscarTipoMantenimiento(Integer id);
}
