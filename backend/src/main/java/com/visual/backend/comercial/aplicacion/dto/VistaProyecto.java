package com.visual.backend.comercial.aplicacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.modelo.ResumenCliente;

public record VistaProyecto(Integer id, Integer idCotizacion, String numeroCotizacion, ResumenCliente cliente,
                            String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin,
                            EstadoProyecto estado, LocalDateTime fechaCreacion) {

    public static VistaProyecto de(Proyecto proyecto) {
        return new VistaProyecto(proyecto.getId(), proyecto.getIdCotizacion(), proyecto.getNumeroCotizacion(),
                proyecto.getCliente(), proyecto.getNombre(), proyecto.getDescripcion(), proyecto.getFechaInicio(),
                proyecto.getFechaFin(), proyecto.getEstado(), proyecto.getFechaCreacion());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", nombre);
        mapa.put("cotizacion", numeroCotizacion);
        mapa.put("estado", estado.name());
        mapa.put("fechaInicio", fechaInicio == null ? null : fechaInicio.toString());
        mapa.put("fechaFin", fechaFin == null ? null : fechaFin.toString());
        return mapa;
    }
}
