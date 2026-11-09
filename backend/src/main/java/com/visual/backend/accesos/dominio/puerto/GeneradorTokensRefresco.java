package com.visual.backend.accesos.dominio.puerto;

/** Genera valores aleatorios de tokens de refresco y su hash para almacenarlos. */
public interface GeneradorTokensRefresco {

    String generar();

    String calcularHash(String token);
}
