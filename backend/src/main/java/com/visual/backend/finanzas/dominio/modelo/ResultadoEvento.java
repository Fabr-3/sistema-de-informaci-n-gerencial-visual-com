package com.visual.backend.finanzas.dominio.modelo;

import java.time.LocalDateTime;

/** Resultado de un evento; idEvento nulo agrupa lo imputado al proyecto sin evento. */
public record ResultadoEvento(Integer idEvento, String evento, LocalDateTime fechaInicio, Margen margen) {
}
