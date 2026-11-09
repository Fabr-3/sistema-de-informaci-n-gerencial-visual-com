package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Intervalo [inicio, fin) de una jornada o asignacion. */
public record Periodo(LocalDateTime inicio, LocalDateTime fin) {

    public Periodo {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ReglaNegocioExcepcion("El fin debe ser posterior al inicio");
        }
    }

    public boolean contiene(Periodo otro) {
        return !otro.inicio.isBefore(inicio) && !otro.fin.isAfter(fin);
    }

    public boolean seCruzaCon(Periodo otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    /** Menor periodo que contiene a ambos. */
    public Periodo abarcar(Periodo otro) {
        return new Periodo(inicio.isBefore(otro.inicio) ? inicio : otro.inicio,
                fin.isAfter(otro.fin) ? fin : otro.fin);
    }
}
