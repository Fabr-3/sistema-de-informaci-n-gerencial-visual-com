package com.visual.backend.accesos.infraestructura.seguridad;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.visual.backend.accesos.dominio.modelo.Usuario;
import com.visual.backend.accesos.dominio.puerto.EmisorTokensAcceso;

/** Emite JWT firmados con HS256 que contienen los permisos efectivos del usuario. */
@Component
public class EmisorTokensJwt implements EmisorTokensAcceso {

    private final JwtEncoder codificadorJwt;
    private final PropiedadesSeguridad.Jwt configuracion;
    private final Clock reloj;

    public EmisorTokensJwt(JwtEncoder codificadorJwt, PropiedadesSeguridad propiedades, Clock reloj) {
        this.codificadorJwt = codificadorJwt;
        this.configuracion = propiedades.jwt();
        this.reloj = reloj;
    }

    @Override
    public TokenAcceso emitir(Usuario usuario) {
        Instant emitidoEn = reloj.instant();
        Instant expiraEn = emitidoEn.plus(configuracion.duracionAcceso());
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .issuer(configuracion.emisor())
                .subject(usuario.getId().toString())
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .claim(ReclamosJwt.NOMBRE_USUARIO, usuario.getNombreUsuario())
                .claim(ReclamosJwt.ROL, usuario.getRol().getCodigo().name())
                .claim(ReclamosJwt.PERMISOS, usuario.permisosEfectivos().stream().sorted().toList())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificadorJwt.encode(JwtEncoderParameters.from(cabecera, reclamos)).getTokenValue();
        return new TokenAcceso(valor, expiraEn);
    }
}
