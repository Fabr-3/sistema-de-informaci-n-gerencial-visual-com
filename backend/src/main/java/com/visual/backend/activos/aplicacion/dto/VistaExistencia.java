package com.visual.backend.activos.aplicacion.dto;

import com.visual.backend.activos.dominio.modelo.ExistenciaLote;

public record VistaExistencia(Integer id, Integer idModelo, String modelo, String categoria, Integer idBodega,
                              String bodega, int cantidad) {

    public static VistaExistencia de(ExistenciaLote existencia) {
        return new VistaExistencia(existencia.getId(), existencia.getModelo().id(),
                existencia.getModelo().nombreCompleto(), existencia.getModelo().categoria().nombre(),
                existencia.getBodega().id(), existencia.getBodega().nombre(), existencia.getCantidad());
    }
}
