package com.visual.backend.accesos.dominio.modelo;

import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Reglas minimas de robustez de contrasenas. El maximo de 72 caracteres se
 * debe a que BCrypt ignora los bytes posteriores.
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 72;
    public static final String DESCRIPCION =
            "La contraseña debe tener entre 8 y 72 caracteres e incluir al menos una letra y un número";

    private static final Pattern CONTIENE_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern CONTIENE_NUMERO = Pattern.compile("\\d");

    private PoliticaContrasena() {
    }

    public static void validar(String contrasena) {
        if (!esValida(contrasena)) {
            throw new ReglaNegocioExcepcion(DESCRIPCION);
        }
    }

    public static boolean esValida(String contrasena) {
        return contrasena != null
                && contrasena.length() >= LONGITUD_MINIMA
                && contrasena.length() <= LONGITUD_MAXIMA
                && CONTIENE_LETRA.matcher(contrasena).find()
                && CONTIENE_NUMERO.matcher(contrasena).find();
    }
}
