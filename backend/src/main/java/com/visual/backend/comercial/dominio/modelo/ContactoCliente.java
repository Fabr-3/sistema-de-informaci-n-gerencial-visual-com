package com.visual.backend.comercial.dominio.modelo;

import java.util.Map;
import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.ReglasTexto;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Medio de contacto del cliente; la referencia indica el area o persona (Gerencia, Compras). */
public record ContactoCliente(Integer id, ElementoCatalogo tipo, String valor, String referencia) {

    /** Formato del valor segun el tipo de contacto (por el nombre del catalogo). */
    private static final Map<String, Pattern> FORMATOS = Map.of(
            "Celular", Pattern.compile("^\\+?\\d{7,15}$"),
            "Teléfono fijo", Pattern.compile("^\\+?[\\d ]{6,20}( int\\. ?\\d{1,5})?$"),
            "Correo electrónico", Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"));

    public ContactoCliente {
        valor = valor == null ? null : valor.trim();
        referencia = referencia == null || referencia.isBlank() ? null : referencia.trim();
    }

    public void validar() {
        Pattern formato = FORMATOS.get(tipo.nombre());
        if (formato != null && !formato.matcher(valor).matches()) {
            throw new ReglaNegocioExcepcion("El " + tipo.nombre().toLowerCase() + " " + valor + " no es válido");
        }
        ReglasTexto.exigirRazonSocial(referencia, "referencia del contacto");
    }
}
