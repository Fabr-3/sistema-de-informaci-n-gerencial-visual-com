package com.visual.backend.finanzas.infraestructura.persistencia.entidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.finanzas.dominio.modelo.EstadoMovimiento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movimientos_financieros")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoFinancieroEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_movimiento", nullable = false, updatable = false)
    private TipoMovimientoEntidad tipo;

    @Column(nullable = false, length = 200, updatable = false)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal monto;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", updatable = false)
    private ProyectoEntidad proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", updatable = false)
    private EventoEntidad evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", updatable = false)
    private ProveedorEntidad proveedor;

    @Column(name = "numero_factura", length = 30, updatable = false)
    private String numeroFactura;

    @Column(name = "id_mantenimiento", updatable = false)
    private Integer idMantenimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoMovimiento estado;

    @Column(name = "motivo_anulacion", columnDefinition = "text")
    private String motivoAnulacion;

    @OneToMany(mappedBy = "movimiento", cascade = CascadeType.ALL)
    private List<VentaDetalleEntidad> detalle = new ArrayList<>();

    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", insertable = false, updatable = false)
    private UsuarioEntidad creador;

    @Column(name = "modificado_por")
    private Integer modificadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
}
