package com.visual.backend.finanzas.infraestructura.persistencia.entidad;

import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;

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
@Table(name = "ventas_detalle")
@Getter
@Setter
@NoArgsConstructor
public class VentaDetalleEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_movimiento", nullable = false, updatable = false)
    private MovimientoFinancieroEntidad movimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activo", updatable = false)
    private ActivoEntidad activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_existencia", updatable = false)
    private ExistenciaLoteEntidad existencia;

    @Column(nullable = false, updatable = false)
    private int cantidad;
}
