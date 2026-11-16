package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.Optional;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.Consumible;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;

public interface RepositorioConsumibles {

    Pagina<Consumible> buscar(Filtros.Consumibles filtro, SolicitudPagina solicitud);

    Optional<Consumible> buscarPorId(Integer id);

    default Consumible obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el consumible", id));
    }

    /** Bloquea la fila hasta el fin de la transaccion para que dos consumos no lean el mismo stock. */
    Consumible obtenerParaActualizar(Integer id);

    boolean existeNombre(String nombre, Integer idExcluido);

    Consumible guardar(Consumible consumible);

    void registrarMovimiento(MovimientoConsumible movimiento);

    Pagina<MovimientoConsumible> movimientos(Integer idConsumible, SolicitudPagina solicitud);
}
