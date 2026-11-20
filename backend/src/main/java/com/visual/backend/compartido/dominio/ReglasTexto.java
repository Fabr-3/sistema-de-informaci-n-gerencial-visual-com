package com.visual.backend.compartido.dominio;

import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Validacion de nombres de personas y razones sociales escritos por el usuario. */
public final class ReglasTexto {

    /** Letras (con tildes y enie), espacios, apostrofos, puntos y guiones; sin numeros. */
    private static final Pattern NOMBRE_PERSONA = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    /** Ademas de letras admite numeros y los signos habituales de una razon social (S.R.L., & Cia.). */
    private static final Pattern RAZON_SOCIAL = Pattern.compile("^[\\p{L}\\d][\\p{L}\\d .,&'()/-]*$");

    private ReglasTexto() {
    }

    /** Exige solo letras: nombres y apellidos, personas naturales y referencias de contacto. */
    public static void exigirNombrePersona(String valor, String campo) {
        if (valor != null && !NOMBRE_PERSONA.matcher(valor.trim()).matches()) {
            throw new ReglaNegocioExcepcion("El campo " + campo + " solo admite letras");
        }
    }

    public static void exigirRazonSocial(String valor, String campo) {
        if (valor != null && !RAZON_SOCIAL.matcher(valor.trim()).matches()) {
            throw new ReglaNegocioExcepcion("El campo " + campo + " contiene caracteres no permitidos");
        }
    }
}
