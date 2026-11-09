package com.visual.backend.logistica.aplicacion.dto;

import jakarta.validation.constraints.Size;

public record ComandoTransporte(@Size(max = 2000) String detallesTransporte) {
}
