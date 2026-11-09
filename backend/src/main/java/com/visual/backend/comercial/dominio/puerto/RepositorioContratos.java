package com.visual.backend.comercial.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

import com.visual.backend.comercial.dominio.modelo.Contrato;

public interface RepositorioContratos {

    List<Contrato> listarPorProyecto(Integer idProyecto);

    Optional<Contrato> buscarPorId(Integer id);

    /** Contrato vigente o excepcion de recurso no encontrado. */
    default Contrato obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el contrato", id));
    }

    int reservarCorrelativo(String prefijo);

    Contrato guardar(Contrato contrato);
}
