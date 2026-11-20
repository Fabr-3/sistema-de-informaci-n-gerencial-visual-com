package com.visual.backend.finanzas.aplicacion.dto;

import java.util.List;

import com.visual.backend.compartido.dominio.ElementoCatalogo;

public record CatalogosProveedores(List<ElementoCatalogo> tiposProveedor, List<ElementoCatalogo> tiposDocumento) {
}
