package com.visual.backend.accesos.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro inmutable: Hibernate nunca emite UPDATE sobre esta entidad. */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre_usuario", length = 50)
    private String nombreUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionAuditoria accion;

    @Column(length = 100)
    private String entidad;

    @Column(name = "id_entidad", length = 50)
    private String idEntidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_anteriores")
    private String valoresAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_nuevos")
    private String valoresNuevos;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "agente_usuario", length = 255)
    private String agenteUsuario;
}
