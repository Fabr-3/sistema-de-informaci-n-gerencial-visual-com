package com.visual.backend.accesos.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Token de refresco persistido. Solo se conserva su hash: el valor original
 * existe unicamente en la cookie del cliente.
 */
public class TokenRefresco {

    private final Long id;
    private final Integer idUsuario;
    private final String hashToken;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaExpiracion;
    private LocalDateTime fechaRevocacion;

    public TokenRefresco(Long id, Integer idUsuario, String hashToken, LocalDateTime fechaCreacion,
                         LocalDateTime fechaExpiracion, LocalDateTime fechaRevocacion) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.hashToken = hashToken;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.fechaRevocacion = fechaRevocacion;
    }

    public static TokenRefresco nuevo(Integer idUsuario, String hashToken, LocalDateTime ahora, LocalDateTime expiracion) {
        return new TokenRefresco(null, idUsuario, hashToken, ahora, expiracion, null);
    }

    public boolean estaVigente(LocalDateTime ahora) {
        return fechaRevocacion == null && fechaExpiracion.isAfter(ahora);
    }

    public boolean estaRevocado() {
        return fechaRevocacion != null;
    }

    public void revocar(LocalDateTime ahora) {
        if (fechaRevocacion == null) {
            fechaRevocacion = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getHashToken() {
        return hashToken;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public LocalDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }
}
