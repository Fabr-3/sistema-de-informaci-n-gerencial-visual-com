package com.visual.backend.accesos.dominio.excepcion;

import com.visual.backend.compartido.dominio.excepcion.ExcepcionDominio;

/**
 * Fallo de autenticacion (credenciales, cuenta no habilitada o sesion
 * invalida). Se responde con 401 y un mensaje que no revela si el usuario
 * existe.
 */
public class AutenticacionExcepcion extends ExcepcionDominio {

    public static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";
    public static final String CUENTA_BLOQUEADA = "Cuenta bloqueada temporalmente por intentos fallidos. Intente más tarde";
    public static final String CUENTA_NO_HABILITADA = "La cuenta no está habilitada. Contacte al administrador";
    public static final String SESION_INVALIDA = "La sesión expiró o no es válida";

    public AutenticacionExcepcion(String mensaje) {
        super(mensaje);
    }
}
