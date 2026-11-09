package com.visual.backend.activos.infraestructura.persistencia.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Synchronize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Vista v_uso_activos: horas de uso calculadas por la base de datos (solo
 * lectura). @Synchronize declara las tablas de las que depende la vista para
 * que Hibernate envie los cambios pendientes antes de consultarla.
 */
@Entity
@Immutable
@Synchronize({"registros_uso", "mantenimientos", "tipos_mantenimiento", "activos"})
@Table(name = "v_uso_activos")
@Getter
@NoArgsConstructor
public class UsoActivoEntidad {

    @Id
    @Column(name = "id_activo")
    private Integer idActivo;

    @Column(name = "horas_totales")
    private BigDecimal horasTotales;

    @Column(name = "horas_desde_mantenimiento")
    private BigDecimal horasDesdeMantenimiento;

    @Column(name = "fecha_ultimo_mantenimiento")
    private LocalDateTime fechaUltimoMantenimiento;
}
