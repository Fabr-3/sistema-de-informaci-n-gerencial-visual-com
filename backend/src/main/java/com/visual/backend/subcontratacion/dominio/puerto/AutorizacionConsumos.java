package com.visual.backend.subcontratacion.dominio.puerto;

/** Alcance del usuario actual sobre los consumos. */
public interface AutorizacionConsumos {

    /** Solo registra consumos (tecnico): queda limitado a los eventos donde esta asignado. */
    boolean limitadoAEventosAsignados();
}
