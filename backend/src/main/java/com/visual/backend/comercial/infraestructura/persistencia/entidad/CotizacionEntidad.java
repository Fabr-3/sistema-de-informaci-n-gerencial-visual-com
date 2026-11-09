package com.visual.backend.comercial.infraestructura.persistencia.entidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cotizaciones")
@Getter
@Setter
@NoArgsConstructor
public class CotizacionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion")
    private Integer id;

    @Column(nullable = false, length = 20, updatable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private ClienteEntidad cliente;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_validez", nullable = false)
    private LocalDate fechaValidez;

    @Column(name = "fecha_evento_inicio")
    private LocalDate fechaEventoInicio;

    @Column(name = "fecha_evento_fin")
    private LocalDate fechaEventoFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "condiciones_comerciales", columnDefinition = "text")
    private String condiciones;

    @ElementCollection
    @CollectionTable(name = "cotizacion_servicios", joinColumns = @JoinColumn(name = "id_cotizacion"))
    private List<LineaCotizacionEmbebida> lineas = new ArrayList<>();

    @Column(name = "resuelto_por")
    private Integer resueltoPor;

    /** Solo lectura: permite mostrar quien resolvio la cotizacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por", insertable = false, updatable = false)
    private UsuarioEntidad resolutor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "motivo_rechazo", columnDefinition = "text")
    private String motivoRechazo;

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
