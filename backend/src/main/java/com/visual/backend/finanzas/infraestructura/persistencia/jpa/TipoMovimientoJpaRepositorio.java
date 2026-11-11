package com.visual.backend.finanzas.infraestructura.persistencia.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visual.backend.finanzas.infraestructura.persistencia.entidad.TipoMovimientoEntidad;

public interface TipoMovimientoJpaRepositorio extends JpaRepository<TipoMovimientoEntidad, Integer> {

    Optional<TipoMovimientoEntidad> findByCodigoSistema(String codigoSistema);
}
