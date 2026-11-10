package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;
import com.visual.backend.finanzas.dominio.puerto.RepositorioTiposMovimiento;
import com.visual.backend.finanzas.infraestructura.persistencia.jpa.TipoMovimientoJpaRepositorio;

@Repository
public class RepositorioTiposMovimientoJpa implements RepositorioTiposMovimiento {

    private static final Sort ORDEN = Sort.by("naturaleza", "id");

    private final TipoMovimientoJpaRepositorio tipos;

    public RepositorioTiposMovimientoJpa(TipoMovimientoJpaRepositorio tipos) {
        this.tipos = tipos;
    }

    @Override
    public List<TipoMovimiento> listar() {
        return tipos.findAll(ORDEN).stream().map(MapeadorFinanzas::aDominio).toList();
    }

    @Override
    public Optional<TipoMovimiento> buscarPorId(Integer id) {
        return tipos.findById(id).map(MapeadorFinanzas::aDominio);
    }
}
