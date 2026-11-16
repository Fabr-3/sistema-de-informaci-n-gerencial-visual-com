package com.visual.backend.logistica.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

/** Documento de identidad del personal tecnico (CI o pasaporte). */
public record DocumentoPersonal(ElementoCatalogo tipo, String numero) {
}
