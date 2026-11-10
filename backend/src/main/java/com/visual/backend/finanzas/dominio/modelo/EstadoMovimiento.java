package com.visual.backend.finanzas.dominio.modelo;

/** Un movimiento no se edita ni se elimina: se anula con un motivo. */
public enum EstadoMovimiento {
    REGISTRADO,
    ANULADO
}
