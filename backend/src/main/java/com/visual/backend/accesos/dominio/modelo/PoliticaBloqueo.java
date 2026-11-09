package com.visual.backend.accesos.dominio.modelo;

import java.time.Duration;

/** Limite de intentos fallidos de inicio de sesion y duracion del bloqueo. */
public record PoliticaBloqueo(int intentosMaximos, Duration duracionBloqueo) {

    public PoliticaBloqueo {
        if (intentosMaximos <= 0 || duracionBloqueo == null || duracionBloqueo.isNegative()) {
            throw new IllegalArgumentException("Politica de bloqueo invalida");
        }
    }
}
