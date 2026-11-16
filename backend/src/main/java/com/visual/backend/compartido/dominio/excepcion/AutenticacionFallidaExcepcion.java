package com.visual.backend.compartido.dominio.excepcion;

/**
 * Base de los fallos de autenticacion. La capa web la responde con 401 sin
 * depender del modulo que la lanza.
 */
public abstract class AutenticacionFallidaExcepcion extends ExcepcionDominio {

    protected AutenticacionFallidaExcepcion(String mensaje) {
        super(mensaje);
    }
}
