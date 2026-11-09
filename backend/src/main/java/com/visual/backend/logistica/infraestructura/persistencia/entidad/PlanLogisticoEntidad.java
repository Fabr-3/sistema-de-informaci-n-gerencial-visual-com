package com.visual.backend.logistica.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "planes_logisticos")
@Getter
@Setter
@NoArgsConstructor
public class PlanLogisticoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Integer id;

    @Column(name = "id_evento", nullable = false, updatable = false)
    private Integer idEvento;

    @Column(name = "detalles_transporte", columnDefinition = "text")
    private String detallesTransporte;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaInicio")
    private List<JornadaEntidad> jornadas = new ArrayList<>();

    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    @Column(name = "modificado_por")
    private Integer modificadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}
