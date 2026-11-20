package com.visual.backend.finanzas.infraestructura.persistencia.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.finanzas.infraestructura.persistencia.entidad.TipoProveedorEntidad;

public interface TipoProveedorJpaRepositorio extends JpaRepository<TipoProveedorEntidad, Integer> {
}
