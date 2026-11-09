package com.visual.backend.activos.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catalogo de solo lectura para la aplicacion. */
@Entity
@Table(name = "tipos_mantenimiento")
@Getter
@NoArgsConstructor
public class TipoMantenimientoEntidad {

    @Id
    @Column(name = "id_tipo_mantenimiento")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "reinicia_horas", nullable = false)
    private boolean reiniciaHoras;
}
