package com.visual.backend.finanzas.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.ReglasDocumento;

/** Documento del proveedor (NIT, carnet con complemento opcional, pasaporte...). */
public record DocumentoProveedor(Integer id, ElementoCatalogo tipo, String numero, String complemento) {

    public DocumentoProveedor {
        numero = ReglasDocumento.normalizarNumero(numero);
        complemento = ReglasDocumento.normalizarComplemento(complemento);
    }

    public String clave() {
        return tipo.id() + ":" + numero + ":" + (complemento == null ? "" : complemento);
    }

    public String descripcion() {
        return ReglasDocumento.descripcion(numero, complemento);
    }
}
