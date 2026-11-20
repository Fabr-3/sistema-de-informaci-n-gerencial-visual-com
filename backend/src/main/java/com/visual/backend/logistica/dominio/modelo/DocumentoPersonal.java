package com.visual.backend.logistica.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.ReglasDocumento;

/** Documento de identidad del personal tecnico (CI con complemento opcional, o pasaporte). */
public record DocumentoPersonal(ElementoCatalogo tipo, String numero, String complemento) {

    public DocumentoPersonal {
        numero = ReglasDocumento.normalizarNumero(numero);
        complemento = ReglasDocumento.normalizarComplemento(complemento);
    }

    public String descripcion() {
        return ReglasDocumento.descripcion(numero, complemento);
    }
}
