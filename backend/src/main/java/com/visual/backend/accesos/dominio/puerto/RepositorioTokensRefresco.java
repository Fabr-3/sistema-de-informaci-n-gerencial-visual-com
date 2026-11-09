package com.visual.backend.accesos.dominio.puerto;

import java.time.LocalDateTime;
import java.util.Optional;

import com.visual.backend.accesos.dominio.modelo.TokenRefresco;

/** Persistencia de tokens de refresco (por hash). */
public interface RepositorioTokensRefresco {

    TokenRefresco guardar(TokenRefresco token);

    Optional<TokenRefresco> buscarPorHash(String hashToken);

    /** Revoca todas las sesiones abiertas del usuario. */
    void revocarTodosDelUsuario(Integer idUsuario, LocalDateTime ahora);
}
