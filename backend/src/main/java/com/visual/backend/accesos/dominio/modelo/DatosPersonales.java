package com.visual.backend.accesos.dominio.modelo;

import java.util.Locale;

/**
 * Datos personales de un usuario ya normalizados: sin espacios sobrantes,
 * email en minusculas y apellido materno opcional (nulo si viene vacio).
 */
public record DatosPersonales(String nombres, String apellidoPaterno, String apellidoMaterno, String email) {

    public DatosPersonales {
        nombres = recortar(nombres);
        apellidoPaterno = recortar(apellidoPaterno);
        apellidoMaterno = vacioANulo(recortar(apellidoMaterno));
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isEmpty() ? null : valor;
    }
}
