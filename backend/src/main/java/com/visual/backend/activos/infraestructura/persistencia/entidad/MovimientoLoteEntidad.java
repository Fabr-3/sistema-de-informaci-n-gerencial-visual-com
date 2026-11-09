package com.visual.backend.activos.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.activos.dominio.modelo.TipoMovimientoLote;

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
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Movimiento historico: nunca se modifica. */
@Entity
@Immutable
@Table(name = "movimientos_lote")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoLoteEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;

    @Column(name = "id_existencia", nullable = false)
    private Integer idExistencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoLote tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 255)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "creado_por")
    private Integer creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", insertable = false, updatable = false)
    private UsuarioEntidad creador;
}
