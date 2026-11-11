package com.visual.backend.gerencial.dominio.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Rango de fechas incluidas en ambos extremos. */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public Periodo {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioExcepcion("Indique las fechas del periodo");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioExcepcion("La fecha inicial no puede ser posterior a la final");
        }
    }

    /** Sin fechas: el mes en curso hasta hoy; sin fecha inicial: el inicio del mes de la final. */
    public static Periodo solicitado(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate fin = hasta == null ? hoy : hasta;
        return new Periodo(desde == null ? fin.withDayOfMonth(1) : desde, fin);
    }

    /** Los doce meses que terminan en la fecha final del periodo. */
    public Periodo anual() {
        return new Periodo(hasta.minusYears(1).plusDays(1), hasta);
    }

    /** El periodo inmediatamente anterior de igual duracion. */
    public Periodo anterior() {
        LocalDate finAnterior = desde.minusDays(1);
        return new Periodo(finAnterior.minusDays(dias() - 1), finAnterior);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }
}
