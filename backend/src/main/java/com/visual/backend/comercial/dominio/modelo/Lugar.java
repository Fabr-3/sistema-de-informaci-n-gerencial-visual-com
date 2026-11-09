package com.visual.backend.comercial.dominio.modelo;

/** Lugar donde se realizan los eventos (hotel, coliseo, recinto ferial). */
public record Lugar(Integer id, String nombre, String direccion, String ciudad) {

    public Lugar {
        nombre = nombre == null ? null : nombre.trim();
        direccion = direccion == null ? null : direccion.trim();
        ciudad = ciudad == null || ciudad.isBlank() ? null : ciudad.trim();
    }
}
