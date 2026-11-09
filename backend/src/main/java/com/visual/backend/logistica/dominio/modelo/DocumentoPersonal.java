package com.visual.backend.logistica.dominio.modelo;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;

/** Documento de identidad del personal tecnico (CI o pasaporte). */
public record DocumentoPersonal(ElementoCatalogo tipo, String numero) {
}
