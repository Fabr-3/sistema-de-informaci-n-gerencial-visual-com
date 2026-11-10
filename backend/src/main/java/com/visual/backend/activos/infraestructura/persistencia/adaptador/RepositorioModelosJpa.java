package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.TipoControl;
import com.visual.backend.activos.dominio.puerto.RepositorioModelos;
import com.visual.backend.activos.infraestructura.persistencia.entidad.EspecificacionEmbebida;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModeloActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ActivoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.CategoriaJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ExistenciaLoteJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.FabricanteJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ModeloActivoJpaRepositorio;

@Repository
public class RepositorioModelosJpa implements RepositorioModelos {

    private final ModeloActivoJpaRepositorio modelos;
    private final FabricanteJpaRepositorio fabricantes;
    private final CategoriaJpaRepositorio categorias;
    private final ActivoJpaRepositorio activos;
    private final ExistenciaLoteJpaRepositorio existencias;

    public RepositorioModelosJpa(ModeloActivoJpaRepositorio modelos, FabricanteJpaRepositorio fabricantes,
                                 CategoriaJpaRepositorio categorias, ActivoJpaRepositorio activos,
                                 ExistenciaLoteJpaRepositorio existencias) {
        this.modelos = modelos;
        this.fabricantes = fabricantes;
        this.categorias = categorias;
        this.activos = activos;
        this.existencias = existencias;
    }

    @Override
    public List<ModeloActivo> listar(TipoControl tipoControl) {
        List<ModeloActivoEntidad> encontrados = tipoControl == null
                ? modelos.findAllByOrderByFabricanteNombreAscNombreAsc()
                : modelos.findByTipoControlOrderByFabricanteNombreAscNombreAsc(tipoControl);
        return encontrados.stream().map(MapeadorActivos::aDominio).toList();
    }

    @Override
    public Optional<ModeloActivo> buscarPorId(Integer id) {
        return modelos.findWithRelacionesById(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existe(Integer idFabricante, String nombre, Integer idExcluido) {
        return idExcluido == null ? modelos.existsByFabricanteIdAndNombreIgnoreCase(idFabricante, nombre)
                : modelos.existsByFabricanteIdAndNombreIgnoreCaseAndIdNot(idFabricante, nombre, idExcluido);
    }

    @Override
    public boolean existePrefijo(String prefijoCodigo, Integer idExcluido) {
        return idExcluido == null ? modelos.existsByPrefijoCodigo(prefijoCodigo)
                : modelos.existsByPrefijoCodigoAndIdNot(prefijoCodigo, idExcluido);
    }

    @Override
    public Map<Integer, Long> contarInventarioPorModelo() {
        Map<Integer, Long> conteo = new HashMap<>();
        activos.contarPorModelo(EstadoOperativo.FUERA_DE_INVENTARIO)
                .forEach(fila -> conteo.put(fila.getIdModelo(), fila.getCantidad()));
        existencias.sumarPorModelo().forEach(fila -> conteo.put(fila.getIdModelo(), fila.getCantidad()));
        return conteo;
    }

    @Override
    public boolean tieneInventario(Integer idModelo) {
        return activos.existsByModeloId(idModelo) || existencias.existsByModeloId(idModelo);
    }

    @Override
    public ModeloActivo guardar(ModeloActivo modelo) {
        ModeloActivoEntidad entidad = modelo.id() == null ? new ModeloActivoEntidad()
                : modelos.getReferenceById(modelo.id());
        entidad.setFabricante(fabricantes.getReferenceById(modelo.fabricante().id()));
        entidad.setCategoria(categorias.getReferenceById(modelo.categoria().id()));
        entidad.setNombre(modelo.nombre());
        entidad.setTipoControl(modelo.tipoControl());
        entidad.setHorasMantenimiento(modelo.horasMantenimiento());
        entidad.setPrefijoCodigo(modelo.prefijoCodigo());
        entidad.getEspecificaciones().clear();
        modelo.especificaciones().forEach(especificacion -> entidad.getEspecificaciones()
                .add(new EspecificacionEmbebida(especificacion.caracteristica(), especificacion.valor())));
        return MapeadorActivos.aDominio(modelos.save(entidad));
    }
}
