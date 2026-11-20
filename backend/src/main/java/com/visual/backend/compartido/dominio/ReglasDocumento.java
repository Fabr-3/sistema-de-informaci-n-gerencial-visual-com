package com.visual.backend.compartido.dominio;

import java.util.Locale;
import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Formato de los documentos de identidad y tributarios segun su tipo (por el
 * nombre del catalogo). Solo el carnet de identidad admite complemento.
 */
public final class ReglasDocumento {

    public static final String CARNET_IDENTIDAD = "Carnet de identidad";
    public static final String NIT = "NIT";
    public static final String PASAPORTE = "Pasaporte";

    /** Numero del carnet con la sigla opcional del departamento de expedicion (4876512 LP). */
    private static final Pattern CARNET = Pattern.compile("^\\d{4,10}( [A-Z]{2})?$");
    /** Complemento del carnet duplicado (1A, 2B...). */
    private static final Pattern COMPLEMENTO = Pattern.compile("^[0-9A-Z]{1,2}$");
    private static final Pattern NUMERO_NIT = Pattern.compile("^\\d{6,15}$");
    private static final Pattern NUMERO_PASAPORTE = Pattern.compile("^[A-Z0-9]{5,15}$");

    private ReglasDocumento() {
    }

    public static boolean esCarnet(ElementoCatalogo tipo) {
        return CARNET_IDENTIDAD.equals(tipo.nombre());
    }

    /** Mayusculas y sin espacios sobrantes; nulo si viene vacio. */
    public static String normalizarComplemento(String complemento) {
        return complemento == null || complemento.isBlank() ? null : complemento.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizarNumero(String numero) {
        return numero == null ? null : numero.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /** Valida el numero (ya normalizado) segun el tipo y que solo el carnet lleve complemento. */
    public static void validar(ElementoCatalogo tipo, String numero, String complemento) {
        if (numero == null || numero.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el número del documento");
        }
        if (complemento != null && !esCarnet(tipo)) {
            throw new ReglaNegocioExcepcion("Solo el carnet de identidad lleva complemento");
        }
        if (complemento != null && !COMPLEMENTO.matcher(complemento).matches()) {
            throw new ReglaNegocioExcepcion("El complemento del carnet tiene 1 o 2 letras o números (por ejemplo, 1A)");
        }
        Pattern formato = switch (tipo.nombre()) {
            case CARNET_IDENTIDAD -> CARNET;
            case NIT -> NUMERO_NIT;
            case PASAPORTE -> NUMERO_PASAPORTE;
            default -> null;
        };
        if (formato != null && !formato.matcher(numero).matches()) {
            throw new ReglaNegocioExcepcion(mensajeFormato(tipo.nombre(), numero));
        }
    }

    /** Texto del documento para mensajes y listados: numero con su complemento. */
    public static String descripcion(String numero, String complemento) {
        return complemento == null ? numero : numero + "-" + complemento;
    }

    private static String mensajeFormato(String tipo, String numero) {
        return switch (tipo) {
            case CARNET_IDENTIDAD -> "El carnet " + numero + " debe tener solo números (4 a 10), con la sigla "
                    + "opcional del departamento (por ejemplo, 4876512 LP)";
            case NIT -> "El NIT " + numero + " debe tener solo números (6 a 15)";
            default -> "El pasaporte " + numero + " debe tener de 5 a 15 letras o números";
        };
    }
}
