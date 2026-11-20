package com.visual.backend.finanzas.dominio.modelo;

import java.util.List;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

/** Los documentos son opcionales (proveedores informales), pero no se repiten. */
public record DatosProveedor(ElementoCatalogo tipo, String razonSocial, String direccion,
                             List<DocumentoProveedor> documentos, String telefono, String email,
                             List<String> servicios) {
}
