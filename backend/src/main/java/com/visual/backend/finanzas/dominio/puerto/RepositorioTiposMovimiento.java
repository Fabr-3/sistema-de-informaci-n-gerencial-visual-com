package com.visual.backend.finanzas.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;

public interface RepositorioTiposMovimiento {

    List<TipoMovimiento> listar();

    Optional<TipoMovimiento> buscarPorId(Integer id);

    default TipoMovimiento obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de movimiento", id));
    }
}
