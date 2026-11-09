package com.visual.backend.compartido.infraestructura.persistencia;

import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Correlativos de codigos con prefijo (ABPL-0001, COT-2026-0001). Toma el
 * mayor numero usado con el prefijo y bloquea la serie hasta el fin de la
 * transaccion (pg_advisory_xact_lock), para que dos operaciones simultaneas
 * no generen el mismo codigo. Debe invocarse dentro de una transaccion.
 */
@Component
public class GeneradorCorrelativos {

    /** Tabla y columna son constantes del codigo, nunca datos del usuario; igual se validan. */
    private static final Pattern IDENTIFICADOR = Pattern.compile("^[a-z_]+$");
    /** Sin comodines de LIKE ni caracteres especiales de expresiones regulares. */
    private static final Pattern PREFIJO = Pattern.compile("^[A-Z0-9-]{2,20}$");

    private final JdbcTemplate jdbc;

    public GeneradorCorrelativos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Mayor correlativo usado para el prefijo (0 si no hay). Los codigos se
     * forman como prefijo + numero; el prefijo incluye su separador final.
     */
    public int reservarUltimo(String tabla, String columna, String prefijo) {
        if (!IDENTIFICADOR.matcher(tabla).matches() || !IDENTIFICADOR.matcher(columna).matches()
                || !PREFIJO.matcher(prefijo).matches()) {
            throw new IllegalArgumentException("Serie de correlativos invalida: " + tabla + "." + columna);
        }
        jdbc.queryForObject("select cast(pg_advisory_xact_lock(hashtext(?)) as text)", String.class,
                tabla + ":" + prefijo);
        String consulta = "select coalesce(max(cast(substring(" + columna + " from ?) as integer)), 0) from "
                + tabla + " where " + columna + " like ?";
        Integer ultimo = jdbc.queryForObject(consulta, Integer.class, "^" + prefijo + "([0-9]+)$", prefijo + "%");
        return ultimo == null ? 0 : ultimo;
    }
}
