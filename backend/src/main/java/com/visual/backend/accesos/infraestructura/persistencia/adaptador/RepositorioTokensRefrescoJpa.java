package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.TokenRefresco;
import com.visual.backend.accesos.dominio.puerto.RepositorioTokensRefresco;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.TokenRefrescoEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.TokenRefrescoJpaRepositorio;

@Repository
public class RepositorioTokensRefrescoJpa implements RepositorioTokensRefresco {

    private final TokenRefrescoJpaRepositorio tokens;

    public RepositorioTokensRefrescoJpa(TokenRefrescoJpaRepositorio tokens) {
        this.tokens = tokens;
    }

    @Override
    public TokenRefresco guardar(TokenRefresco token) {
        TokenRefrescoEntidad entidad = token.getId() == null
                ? new TokenRefrescoEntidad()
                : tokens.getReferenceById(token.getId());
        MapeadorAccesos.copiar(token, entidad);
        return MapeadorAccesos.aDominio(tokens.save(entidad));
    }

    @Override
    public Optional<TokenRefresco> buscarPorHash(String hashToken) {
        return tokens.findByHashToken(hashToken).map(MapeadorAccesos::aDominio);
    }

    @Override
    public void revocarTodosDelUsuario(Integer idUsuario, LocalDateTime ahora) {
        tokens.revocarVigentesDelUsuario(idUsuario, ahora);
    }
}
