package com.visual.backend.accesos.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catalogo fijo: solo lectura desde la aplicacion. */
@Entity
@Table(name = "permisos")
@Getter
@NoArgsConstructor
public class PermisoEntidad {

    @Id
    @Column(name = "id_permiso")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;
}
