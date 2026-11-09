package com.visual.backend.compartido.dominio.excepcion;

/** Se lanza cuando una operacion viola una regla de negocio. */
public class ReglaNegocioExcepcion extends ExcepcionDominio {

    public ReglaNegocioExcepcion(String mensaje) {
        super(mensaje);
    }
}
