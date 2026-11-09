package com.visual.backend.activos.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.ExistenciaLote;
import com.visual.backend.activos.dominio.modelo.FiltroExistencias;
import com.visual.backend.activos.dominio.modelo.MovimientoLote;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Existencias y movimientos de los activos controlados por lote. */
public interface RepositorioLotes {

    List<ExistenciaLote> buscar(FiltroExistencias filtro);

    Optional<ExistenciaLote> buscarPorId(Integer id);

    /**
     * Busca la existencia del modelo en la bodega bloqueandola hasta el fin de
     * la transaccion, para que dos movimientos simultaneos no la dejen negativa.
     */
    Optional<ExistenciaLote> buscarParaActualizar(Integer idModelo, Integer idBodega);

    ExistenciaLote guardar(ExistenciaLote existencia);

    MovimientoLote registrarMovimiento(MovimientoLote movimiento);

    Pagina<MovimientoLote> listarMovimientos(Integer idExistencia, SolicitudPagina solicitud);
}
