package com.visual.backend.subcontratacion.dominio.modelo;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Proveedor del modulo financiero; vigente si no fue eliminado. */
public record ProveedorReferencia(Integer id, String razonSocial, boolean vigente) {

    public ElementoCatalogo exigirVigente() {
        if (!vigente) {
            throw new ReglaNegocioExcepcion("El proveedor " + razonSocial + " ya no está vigente");
        }
        return new ElementoCatalogo(id, razonSocial);
    }
}
