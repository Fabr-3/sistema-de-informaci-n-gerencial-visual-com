package com.visual.backend.comercial.dominio.modelo;

/**
 * Estados de una cotizacion. Solo el borrador se edita; el gerente la acepta
 * o rechaza, y el sistema la marca vencida al pasar su fecha de validez.
 */
public enum EstadoCotizacion {
    BORRADOR,
    ACEPTADA,
    RECHAZADA,
    VENCIDA
}
