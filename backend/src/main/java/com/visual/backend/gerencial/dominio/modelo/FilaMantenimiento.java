package com.visual.backend.gerencial.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mantenimiento del reporte; fin, condicion y costo son nulos mientras esta en proceso. */
public record FilaMantenimiento(String codigo, String modelo, String tipo, LocalDateTime inicio, LocalDateTime fin,
                                String estado, String condicion, BigDecimal costo) {
}
