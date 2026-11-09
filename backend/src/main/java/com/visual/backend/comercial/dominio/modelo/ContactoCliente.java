package com.visual.backend.comercial.dominio.modelo;

/** Medio de contacto del cliente; la referencia indica el area o persona (Gerencia, Compras). */
public record ContactoCliente(Integer id, ElementoCatalogo tipo, String valor, String referencia) {

    public ContactoCliente {
        valor = valor == null ? null : valor.trim();
        referencia = referencia == null || referencia.isBlank() ? null : referencia.trim();
    }
}
