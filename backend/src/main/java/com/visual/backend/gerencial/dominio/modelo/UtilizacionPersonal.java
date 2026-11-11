package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;

/** Dias con asignacion de un tecnico en el periodo, con sus eventos y horas asignadas. */
public record UtilizacionPersonal(Integer idPersonal, String nombre, TasaUtilizacion tasa, int eventos,
                                  BigDecimal horas) {
}
