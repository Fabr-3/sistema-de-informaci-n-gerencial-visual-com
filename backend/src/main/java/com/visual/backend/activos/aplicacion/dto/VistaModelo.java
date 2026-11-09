package com.visual.backend.activos.aplicacion.dto;

import java.util.List;

import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Especificacion;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.TipoControl;

/**
 * Modelo con su cantidad en inventario: unidades vigentes no dadas de baja
 * (por unidad) o suma de existencias en todas las bodegas (por lote).
 */
public record VistaModelo(Integer id, String nombre, Fabricante fabricante, Categoria categoria,
                          TipoControl tipoControl, Integer horasMantenimiento, String prefijoCodigo,
                          List<Especificacion> especificaciones, long cantidadInventario) {

    public static VistaModelo de(ModeloActivo modelo, long cantidadInventario) {
        return new VistaModelo(modelo.id(), modelo.nombre(), modelo.fabricante(), modelo.categoria(),
                modelo.tipoControl(), modelo.horasMantenimiento(), modelo.prefijoCodigo(),
                modelo.especificaciones(), cantidadInventario);
    }
}
