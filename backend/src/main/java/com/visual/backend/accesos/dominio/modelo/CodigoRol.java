package com.visual.backend.accesos.dominio.modelo;

/** Roles fijos del sistema. Deben coincidir con roles.codigo en la base de datos. */
public enum CodigoRol {
    ADMINISTRADOR,
    GERENTE,
    TECNICO,
    SUBCONTRATADO_TEMPORAL;

    /** Los sub-contratados solo trabajan durante una vigencia definida. */
    public boolean requiereFechaExpiracion() {
        return this == SUBCONTRATADO_TEMPORAL;
    }
}
