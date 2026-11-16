package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.ConsumoProyecto;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;

public interface RepositorioConsumos {

    Pagina<ConsumoProyecto> buscar(Filtros.Consumos filtro, SolicitudPagina solicitud);

    Optional<ConsumoProyecto> buscarPorId(Integer id);

    default ConsumoProyecto obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el consumo", id));
    }

    ConsumoProyecto guardar(ConsumoProyecto consumo);
}
