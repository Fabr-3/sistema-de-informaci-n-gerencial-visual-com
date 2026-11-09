package com.visual.backend.comercial.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catalogo de solo lectura para la aplicacion. */
@Entity
@Table(name = "tipos_cliente")
@Getter
@NoArgsConstructor
public class TipoClienteEntidad {

    @Id
    @Column(name = "id_tipo_cliente")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;
}
