package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDateTime;

public record DatosEvento(Lugar lugar, String nombre, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
}
