package com.visual.backend.compartido.dominio.excepcion;

/** Se lanza cuando la entidad solicitada no existe o fue eliminada. */
public class RecursoNoEncontradoExcepcion extends ExcepcionDominio {

    public RecursoNoEncontradoExcepcion(String entidad, Object identificador) {
        super("No se encontró " + entidad + " con identificador " + identificador);
    }
}
