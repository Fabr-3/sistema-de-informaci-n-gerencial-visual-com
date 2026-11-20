package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.GastoEvento;

public interface RepositorioGastos {

    Pagina<GastoEvento> buscar(Filtros.Gastos filtro, SolicitudPagina solicitud);

    Optional<GastoEvento> buscarPorId(Integer id);

    default GastoEvento obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el gasto del evento", id));
    }

    /** Inserta el gasto nuevo o guarda su anulacion. */
    GastoEvento guardar(GastoEvento gasto);
}
