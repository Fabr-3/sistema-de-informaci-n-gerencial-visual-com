package com.visual.backend.compartido.infraestructura.persistencia;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Utilidades comunes de los adaptadores JPA de todos los modulos. */
public final class ConsultasJpa {

    /** Caracter de escape para LIKE: evita que % y _ del usuario actuen como comodines. */
    public static final char ESCAPE_LIKE = '\\';

    private ConsultasJpa() {
    }

    public static Pageable paginacion(SolicitudPagina solicitud, Sort orden) {
        return PageRequest.of(solicitud.pagina(), solicitud.tamano(), orden);
    }

    public static <T> Pagina<T> pagina(Page<T> pagina) {
        return new Pagina<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    /** Patron "contiene" en minusculas con los comodines escapados. */
    public static String patronContiene(String texto) {
        String escapado = texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
