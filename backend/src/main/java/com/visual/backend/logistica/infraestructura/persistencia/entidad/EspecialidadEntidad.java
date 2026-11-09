package com.visual.backend.logistica.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "especialidades_tecnicas")
@Getter
@NoArgsConstructor
public class EspecialidadEntidad {

    @Id
    @Column(name = "id_especialidad")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;
}
