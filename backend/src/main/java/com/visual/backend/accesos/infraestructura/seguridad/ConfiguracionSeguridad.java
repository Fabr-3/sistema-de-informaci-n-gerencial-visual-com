package com.visual.backend.accesos.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.visual.backend.accesos.infraestructura.web.ControladorAutenticacion;

/**
 * API sin estado protegida con JWT. CSRF se desactiva porque la autenticacion
 * viaja en la cabecera Authorization; la cookie de refresco es SameSite=Strict
 * y solo se envia a /api/auth.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            ControladorAutenticacion.RUTA_INICIO_SESION,
            ControladorAutenticacion.RUTA_REFRESCO,
            ControladorAutenticacion.RUTA_CIERRE_SESION);

    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.POST, RUTAS_PUBLICAS.toArray(String[]::new)).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .bearerTokenResolver(resolutorTokenSinRutasPublicas())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorAutenticacion())));
        return http.build();
    }

    @Bean
    public JwtEncoder codificadorJwt(PropiedadesSeguridad propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveFirma(propiedades)));
    }

    @Bean
    public JwtDecoder decodificadorJwt(PropiedadesSeguridad propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveFirma(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.jwt().emisor()));
        return decodificador;
    }

    /** Los permisos del reclamo "permisos" se usan tal cual como autoridades. */
    private static JwtAuthenticationConverter convertidorAutenticacion() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(ReclamosJwt.PERMISOS);
        autoridades.setAuthorityPrefix("");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    /**
     * Ignora el token en las rutas publicas: un token de acceso vencido no debe
     * impedir refrescar la sesion ni cerrarla.
     */
    private static BearerTokenResolver resolutorTokenSinRutasPublicas() {
        DefaultBearerTokenResolver resolutor = new DefaultBearerTokenResolver();
        return solicitud -> RUTAS_PUBLICAS.contains(solicitud.getRequestURI()) ? null : resolutor.resolve(solicitud);
    }

    private static SecretKey claveFirma(PropiedadesSeguridad propiedades) {
        return new SecretKeySpec(propiedades.jwt().secreto().getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC);
    }
}
