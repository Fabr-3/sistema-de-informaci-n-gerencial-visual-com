package com.visual.backend.logistica.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import com.visual.backend.logistica.dominio.modelo.EstadoReserva;

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

/** Reserva de un activo; guarda su id para no depender de las entidades del modulo de activos. */
@Entity
@Table(name = "reservas_activo")
@Getter
@Setter
@NoArgsConstructor
public class ReservaActivoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer id;

    @Column(name = "id_plan", nullable = false, updatable = false)
    private Integer idPlan;

    @Column(name = "id_activo", nullable = false, updatable = false)
    private Integer idActivo;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDateTime fechaSalida;

    @Column(name = "fecha_retorno", nullable = false)
    private LocalDateTime fechaRetorno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReserva estado;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    @Column(name = "motivo_liberacion", length = 255)
    private String motivoLiberacion;

    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "modificado_por")
    private Integer modificadoPor;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
}
