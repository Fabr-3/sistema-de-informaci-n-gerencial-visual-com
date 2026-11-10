package com.visual.backend.finanzas.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.TotalesMovimientos;

public interface RepositorioMovimientos {

    Optional<MovimientoFinanciero> buscarPorId(Integer id);

    default MovimientoFinanciero obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el movimiento", id));
    }

    Pagina<MovimientoFinanciero> buscar(Filtros.Movimientos filtro, SolicitudPagina solicitud);

    /** Totales de los movimientos registrados que cumplen el filtro (ignora el filtro de estado). */
    TotalesMovimientos totales(Filtros.Movimientos filtro);

    MovimientoFinanciero guardar(MovimientoFinanciero movimiento);
}
