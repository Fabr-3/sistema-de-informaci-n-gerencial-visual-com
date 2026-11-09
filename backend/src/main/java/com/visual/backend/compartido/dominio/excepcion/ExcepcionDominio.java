package com.visual.backend.compartido.dominio.excepcion;

/**
 * Base de las excepciones de negocio. La capa web las traduce a respuestas
 * HTTP sin exponer detalles internos.
 */
public abstract class ExcepcionDominio extends RuntimeException {

    protected ExcepcionDominio(String mensaje) {
        super(mensaje);
    }
}
