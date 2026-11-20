package com.visual.backend.logistica.dominio.modelo;

/** Unidades libres de un modelo controlado por unidad durante el periodo de la reserva. */
public record DisponibilidadModelo(Integer idModelo, String modelo, String categoria, int disponibles) {
}
