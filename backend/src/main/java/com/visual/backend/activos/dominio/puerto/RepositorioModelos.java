package com.visual.backend.activos.dominio.puerto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.TipoControl;

public interface RepositorioModelos {

    /** Modelos ordenados por fabricante y nombre; tipoControl nulo = todos. */
    List<ModeloActivo> listar(TipoControl tipoControl);

    Optional<ModeloActivo> buscarPorId(Integer id);

    boolean existe(Integer idFabricante, String nombre, Integer idExcluido);

    boolean existePrefijo(String prefijoCodigo, Integer idExcluido);

    /**
     * Cantidad en inventario por id de modelo: unidades vigentes no dadas de
     * baja (por unidad) o suma de existencias (por lote).
     */
    Map<Integer, Long> contarInventarioPorModelo();

    /** Indica si el modelo ya tiene activos o existencias registradas. */
    boolean tieneInventario(Integer idModelo);

    ModeloActivo guardar(ModeloActivo modelo);
}
