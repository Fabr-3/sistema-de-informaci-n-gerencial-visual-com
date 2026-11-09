package com.visual.backend.respaldos.dominio;

/** Fallo tecnico al generar o leer un respaldo (no es una regla de negocio). */
public class RespaldoExcepcion extends RuntimeException {

    public RespaldoExcepcion(String mensaje) {
        super(mensaje);
    }

    public RespaldoExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
