package com.visual.backend.accesos.dominio.puerto;

import java.time.Instant;

import com.visual.backend.accesos.dominio.modelo.Usuario;

/** Emite tokens de acceso de corta duracion con los permisos del usuario. */
public interface EmisorTokensAcceso {

    TokenAcceso emitir(Usuario usuario);

    record TokenAcceso(String valor, Instant expiraEn) {
    }
}
