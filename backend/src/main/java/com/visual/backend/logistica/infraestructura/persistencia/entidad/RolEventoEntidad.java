package com.visual.backend.logistica.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles_evento")
@Getter
@NoArgsConstructor
public class RolEventoEntidad {

    @Id
    @Column(name = "id_rol_evento")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;
}
