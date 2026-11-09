package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.activos.infraestructura.persistencia.entidad.BodegaEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.CategoriaEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.FabricanteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.BodegaJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.CategoriaJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.FabricanteJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.TipoMantenimientoJpaRepositorio;

@Repository
public class RepositorioCatalogosActivosJpa implements RepositorioCatalogosActivos {

    private final CategoriaJpaRepositorio categorias;
    private final FabricanteJpaRepositorio fabricantes;
    private final BodegaJpaRepositorio bodegas;
    private final TipoMantenimientoJpaRepositorio tiposMantenimiento;
    private final Clock reloj;

    public RepositorioCatalogosActivosJpa(CategoriaJpaRepositorio categorias, FabricanteJpaRepositorio fabricantes,
                                          BodegaJpaRepositorio bodegas,
                                          TipoMantenimientoJpaRepositorio tiposMantenimiento, Clock reloj) {
        this.categorias = categorias;
        this.fabricantes = fabricantes;
        this.bodegas = bodegas;
        this.tiposMantenimiento = tiposMantenimiento;
        this.reloj = reloj;
    }

    @Override
    public List<Categoria> listarCategorias() {
        return categorias.findAllByOrderByNombreAsc().stream().map(MapeadorActivos::aDominio).toList();
    }

    @Override
    public Optional<Categoria> buscarCategoria(Integer id) {
        return categorias.findById(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeCategoria(String nombre, Integer idExcluido) {
        return idExcluido == null ? categorias.existsByNombreIgnoreCase(nombre)
                : categorias.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
    }

    @Override
    public Categoria guardarCategoria(Categoria categoria) {
        CategoriaEntidad entidad = categoria.id() == null ? new CategoriaEntidad()
                : categorias.getReferenceById(categoria.id());
        entidad.setNombre(categoria.nombre());
        return MapeadorActivos.aDominio(categorias.save(entidad));
    }

    @Override
    public List<Fabricante> listarFabricantes() {
        return fabricantes.findAllByOrderByNombreAsc().stream().map(MapeadorActivos::aDominio).toList();
    }

    @Override
    public Optional<Fabricante> buscarFabricante(Integer id) {
        return fabricantes.findById(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeFabricante(String nombre, Integer idExcluido) {
        return idExcluido == null ? fabricantes.existsByNombreIgnoreCase(nombre)
                : fabricantes.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
    }

    @Override
    public Fabricante guardarFabricante(Fabricante fabricante) {
        FabricanteEntidad entidad = fabricante.id() == null ? new FabricanteEntidad()
                : fabricantes.getReferenceById(fabricante.id());
        entidad.setNombre(fabricante.nombre());
        return MapeadorActivos.aDominio(fabricantes.save(entidad));
    }

    @Override
    public List<Bodega> listarBodegas() {
        return bodegas.findAllByOrderByNombreAsc().stream().map(MapeadorActivos::aDominio).toList();
    }

    @Override
    public Optional<Bodega> buscarBodega(Integer id) {
        return bodegas.findById(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeBodega(String nombre, Integer idExcluido) {
        return idExcluido == null ? bodegas.existsByNombreIgnoreCase(nombre)
                : bodegas.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
    }

    @Override
    public Bodega guardarBodega(Bodega bodega, Integer actor) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        BodegaEntidad entidad;
        if (bodega.id() == null) {
            entidad = new BodegaEntidad();
            entidad.setCreadoPor(actor);
            entidad.setFechaCreacion(ahora);
        } else {
            entidad = bodegas.getReferenceById(bodega.id());
            entidad.setModificadoPor(actor);
            entidad.setFechaModificacion(ahora);
        }
        entidad.setNombre(bodega.nombre());
        entidad.setDireccion(bodega.direccion());
        entidad.setActiva(bodega.activa());
        return MapeadorActivos.aDominio(bodegas.save(entidad));
    }

    @Override
    public List<TipoMantenimiento> listarTiposMantenimiento() {
        return tiposMantenimiento.findAllByOrderByNombreAsc().stream().map(MapeadorActivos::aDominio).toList();
    }

    @Override
    public Optional<TipoMantenimiento> buscarTipoMantenimiento(Integer id) {
        return tiposMantenimiento.findById(id).map(MapeadorActivos::aDominio);
    }
}
