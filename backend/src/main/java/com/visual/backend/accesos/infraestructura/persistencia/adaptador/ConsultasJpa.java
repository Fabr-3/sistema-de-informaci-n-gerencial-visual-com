package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Utilidades comunes de los adaptadores JPA. */
final class ConsultasJpa {

    /** Caracter de escape para LIKE: evita que % y _ del usuario actuen como comodines. */
    static final char ESCAPE_LIKE = '\\';

    private ConsultasJpa() {
    }

    static Pageable paginacion(SolicitudPagina solicitud, Sort orden) {
        return PageRequest.of(solicitud.pagina(), solicitud.tamano(), orden);
    }

    static <T> Pagina<T> pagina(Page<T> pagina) {
        return new Pagina<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    /** Patron "contiene" en minusculas con los comodines escapados. */
    static String patronContiene(String texto) {
        String escapado = texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
