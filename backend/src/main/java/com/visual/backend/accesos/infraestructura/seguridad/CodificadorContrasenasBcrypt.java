package com.visual.backend.accesos.infraestructura.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.visual.backend.accesos.dominio.puerto.CodificadorContrasenas;

@Component
public class CodificadorContrasenasBcrypt implements CodificadorContrasenas {

    /** Factor de costo de BCrypt: equilibrio entre seguridad y tiempo de respuesta. */
    private static final int FACTOR_COSTO = 12;

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(FACTOR_COSTO);

    @Override
    public String codificar(String contrasenaPlana) {
        return codificador.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String hash) {
        return codificador.matches(contrasenaPlana, hash);
    }
}
