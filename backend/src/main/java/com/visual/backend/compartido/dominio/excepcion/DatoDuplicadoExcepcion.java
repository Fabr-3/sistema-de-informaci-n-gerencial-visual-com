package com.visual.backend.compartido.dominio.excepcion;

/** Se lanza cuando un dato que debe ser unico ya esta registrado. */
public class DatoDuplicadoExcepcion extends ReglaNegocioExcepcion {

    public DatoDuplicadoExcepcion(String mensaje) {
        super(mensaje);
    }
}
