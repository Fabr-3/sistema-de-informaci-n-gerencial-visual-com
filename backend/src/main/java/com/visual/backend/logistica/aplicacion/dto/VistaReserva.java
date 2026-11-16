package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.EstadoReserva;
import com.visual.backend.logistica.dominio.modelo.ReservaActivo;

public record VistaReserva(Integer id, Integer idActivo, String codigo, String modelo, String categoria,
                           LocalDateTime salida, LocalDateTime retorno, EstadoReserva estado) {

    public static VistaReserva de(ReservaActivo reserva, ActivoReservable activo) {
        return new VistaReserva(reserva.getId(), reserva.getIdActivo(), activo.codigo(), activo.modelo(),
                activo.categoria(), reserva.getPeriodo().salida(), reserva.getPeriodo().retorno(),
                reserva.getEstado());
    }

    public static Map<String, Object> mapaAuditoria(ReservaActivo reserva) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("plan", reserva.getIdPlan());
        mapa.put("activo", reserva.getIdActivo());
        mapa.put("salida", reserva.getPeriodo().salida().toString());
        mapa.put("retorno", reserva.getPeriodo().retorno().toString());
        mapa.put("estado", reserva.getEstado().name());
        mapa.put("motivoLiberacion", reserva.getMotivoLiberacion());
        return mapa;
    }
}
