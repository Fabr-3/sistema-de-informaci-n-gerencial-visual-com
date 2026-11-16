package com.visual.backend.logistica.aplicacion.dto;

import java.util.List;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;

public record CatalogosLogistica(List<ElementoCatalogo> rolesEvento, List<ElementoCatalogo> especialidades,
                                 List<ElementoCatalogo> tiposDocumento, List<UsuarioVinculado> usuarios) {
}
