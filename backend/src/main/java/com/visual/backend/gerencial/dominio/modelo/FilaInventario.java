package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;

/**
 * Equipos controlados por unidad de una categoria, por estado. valor es el
 * costo de adquisicion de los que siguen en el inventario.
 */
public record FilaInventario(String categoria, int total, int disponibles, int enUso, int enMantenimiento,
                             int deBaja, int vendidos, BigDecimal valor) {
}
