package com.visual.backend.activos.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registros_uso")
@Getter
@Setter
@NoArgsConstructor
public class RegistroUsoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_uso")
    private Integer id;

    @Column(name = "id_activo", nullable = false, updatable = false)
    private Integer idActivo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    /** Solo lectura: permite mostrar quien registro el uso. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", insertable = false, updatable = false)
    private UsuarioEntidad creador;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "eliminado_por")
    private Integer eliminadoPor;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}
