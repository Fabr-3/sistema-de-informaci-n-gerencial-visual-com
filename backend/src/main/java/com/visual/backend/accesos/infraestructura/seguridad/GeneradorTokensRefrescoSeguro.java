package com.visual.backend.accesos.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.visual.backend.accesos.dominio.puerto.GeneradorTokensRefresco;

/**
 * Tokens opacos de 256 bits aleatorios. Se almacenan como SHA-256: al ser
 * aleatorios y largos no requieren un hash lento como BCrypt.
 */
@Component
public class GeneradorTokensRefrescoSeguro implements GeneradorTokensRefresco {

    private static final int BYTES_TOKEN = 32;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String calcularHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITMO_HASH).digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no soporta " + ALGORITMO_HASH, e);
        }
    }
}
