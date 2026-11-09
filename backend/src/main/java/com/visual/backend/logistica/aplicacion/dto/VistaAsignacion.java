package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.dominio.modelo.ResumenPersonal;

/** Asignacion con sus cruces con otros eventos (alertas, no bloqueos). */
public record VistaAsignacion(Integer id, ResumenPersonal personal, ElementoCatalogo rol, LocalDateTime inicio,
                              LocalDateTime fin, List<Cruce> cruces) {

    public static VistaAsignacion de(Asignacion asignacion, List<Cruce> cruces) {
        return new VistaAsignacion(asignacion.getId(), asignacion.getPersonal(), asignacion.getRol(),
                asignacion.getPeriodo().inicio(), asignacion.getPeriodo().fin(), cruces);
    }

    public static Map<String, Object> mapaAuditoria(Asignacion asignacion) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("plan", asignacion.getIdPlan());
        mapa.put("personal", asignacion.getPersonal().nombreCompleto());
        mapa.put("rol", asignacion.getRol().nombre());
        mapa.put("inicio", asignacion.getPeriodo().inicio().toString());
        mapa.put("fin", asignacion.getPeriodo().fin().toString());
        return mapa;
    }
}
