package com.visual.backend.accesos.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tokens_refresco")
@Getter
@Setter
@NoArgsConstructor
public class TokenRefrescoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Integer idUsuario;

    @Column(name = "hash_token", nullable = false, length = 64, updatable = false)
    private String hashToken;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, updatable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;
}
