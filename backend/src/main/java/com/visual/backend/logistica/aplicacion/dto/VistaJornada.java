package com.visual.backend.logistica.aplicacion.dto;

import java.time.LocalDateTime;

import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.TipoJornada;

public record VistaJornada(Integer id, TipoJornada tipo, LocalDateTime inicio, LocalDateTime fin,
                           String descripcion) {

    public static VistaJornada de(Jornada jornada) {
        return new VistaJornada(jornada.id(), jornada.tipo(), jornada.periodo().inicio(), jornada.periodo().fin(),
                jornada.descripcion());
    }
}
