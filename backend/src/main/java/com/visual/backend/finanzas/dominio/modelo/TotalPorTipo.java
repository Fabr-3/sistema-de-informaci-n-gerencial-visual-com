package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;

public record TotalPorTipo(String tipo, Naturaleza naturaleza, BigDecimal monto) {
}
