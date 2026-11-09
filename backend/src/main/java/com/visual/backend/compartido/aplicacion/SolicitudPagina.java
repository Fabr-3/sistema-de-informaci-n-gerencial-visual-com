package com.visual.backend.compartido.aplicacion;

/**
 * Parametros de paginacion. El tamano se acota para evitar consultas
 * excesivamente grandes.
 */
public record SolicitudPagina(int pagina, int tamano) {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    public SolicitudPagina {
        pagina = Math.max(pagina, 0);
        tamano = tamano <= 0 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
    }
}
