package com.visual.backend.comercial.dominio.modelo;

/** Documento de identidad o tributario de un cliente (CI, NIT, ...). */
public record DocumentoCliente(Integer id, ElementoCatalogo tipo, String numero) {

    public DocumentoCliente {
        numero = numero == null ? null : numero.trim();
    }
}
