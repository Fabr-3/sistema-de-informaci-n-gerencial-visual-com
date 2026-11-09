package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos editables de un activo. */
public record DatosActivo(String codigoInventario, String numeroSerie, LocalDate fechaAdquisicion,
                          BigDecimal costoAdquisicion, String observaciones) {
}
