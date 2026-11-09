package com.visual.backend.activos.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EspecificacionEmbebida {

    @Column(nullable = false, length = 100)
    private String caracteristica;

    @Column(nullable = false, length = 150)
    private String valor;
}
