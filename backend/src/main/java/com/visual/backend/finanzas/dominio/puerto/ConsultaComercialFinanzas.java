package com.visual.backend.finanzas.dominio.puerto;

import java.util.Optional;

import com.visual.backend.finanzas.dominio.modelo.Referencia;

/** Lectura de proyectos y eventos del modulo comercial para imputar movimientos. */
public interface ConsultaComercialFinanzas {

    Optional<Referencia> buscarProyecto(Integer idProyecto);

    /** El evento solo se devuelve si pertenece al proyecto indicado. */
    Optional<Referencia> buscarEventoDelProyecto(Integer idEvento, Integer idProyecto);
}
