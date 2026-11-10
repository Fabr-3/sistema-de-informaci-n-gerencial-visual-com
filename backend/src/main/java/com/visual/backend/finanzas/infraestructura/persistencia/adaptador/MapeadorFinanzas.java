package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.List;

import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.DocumentoProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;

/** Conversion de entidades JPA a modelos de dominio del modulo financiero. */
final class MapeadorFinanzas {

    private MapeadorFinanzas() {
    }

    static Proveedor aDominio(ProveedorEntidad entidad) {
        DocumentoProveedorEntidad nit = documentoNit(entidad);
        return Proveedor.builder()
                .id(entidad.getId())
                .razonSocial(entidad.getRazonSocial())
                .nit(nit == null ? null : nit.getNumero())
                .telefono(entidad.getTelefono())
                .email(entidad.getEmail())
                .servicios(List.copyOf(entidad.getServicios()))
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .fechaEliminacion(entidad.getFechaEliminacion())
                .build();
    }

    static DocumentoProveedorEntidad documentoNit(ProveedorEntidad entidad) {
        return entidad.getDocumentos().stream()
                .filter(documento -> documento.getFechaEliminacion() == null
                        && RepositorioProveedoresJpa.TIPO_NIT.equals(documento.getTipo().getNombre()))
                .findFirst().orElse(null);
    }
}
