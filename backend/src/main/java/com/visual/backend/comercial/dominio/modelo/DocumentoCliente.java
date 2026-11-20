package com.visual.backend.comercial.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.ReglasDocumento;

/** Documento de identidad o tributario de un cliente (CI con complemento opcional, NIT, ...). */
public record DocumentoCliente(Integer id, ElementoCatalogo tipo, String numero, String complemento) {

    public DocumentoCliente {
        numero = ReglasDocumento.normalizarNumero(numero);
        complemento = ReglasDocumento.normalizarComplemento(complemento);
    }

    /** Clave que identifica al documento: tipo, numero y complemento. */
    public String clave() {
        return tipo.id() + ":" + numero + ":" + (complemento == null ? "" : complemento);
    }

    public String descripcion() {
        return ReglasDocumento.descripcion(numero, complemento);
    }
}
