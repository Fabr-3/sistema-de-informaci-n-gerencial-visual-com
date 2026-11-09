package com.visual.backend.logistica.dominio.modelo;

import java.util.List;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;

/** Datos editables del personal tecnico (usuario nulo = sin cuenta del sistema). */
public record DatosPersonal(String nombres, String apellidos, String telefono, DocumentoPersonal documento,
                            List<ElementoCatalogo> especialidades, UsuarioVinculado usuario, boolean activo) {
}
