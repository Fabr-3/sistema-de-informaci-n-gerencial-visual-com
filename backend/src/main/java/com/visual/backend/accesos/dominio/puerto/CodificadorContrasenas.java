package com.visual.backend.accesos.dominio.puerto;

/** Hash unidireccional de contrasenas. */
public interface CodificadorContrasenas {

    String codificar(String contrasenaPlana);

    boolean coincide(String contrasenaPlana, String hash);
}
