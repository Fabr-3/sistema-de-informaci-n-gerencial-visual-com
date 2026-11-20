package com.visual.backend.logistica.dominio.puerto;

import java.util.List;

import com.visual.backend.logistica.dominio.modelo.EquipoSubalquilado;

/** Lectura de los subalquileres que el modulo de subcontratacion registro para un evento. */
public interface ConsultaSubalquileres {

    /** Solo los vigentes (no anulados). */
    List<EquipoSubalquilado> delEvento(Integer idEvento);
}
