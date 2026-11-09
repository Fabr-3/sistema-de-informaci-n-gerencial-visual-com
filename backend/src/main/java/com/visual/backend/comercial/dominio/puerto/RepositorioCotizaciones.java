package com.visual.backend.comercial.dominio.puerto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

public interface RepositorioCotizaciones {

    Optional<Cotizacion> buscarPorId(Integer id);

    /** Cotizacion vigente o excepcion de recurso no encontrado. */
    default Cotizacion obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la cotización", id));
    }

    Pagina<Cotizacion> buscar(Filtros.Cotizaciones filtro, SolicitudPagina solicitud);

    /** Borradores cuya fecha de validez es anterior a hoy. */
    List<Cotizacion> buscarBorradoresCaducados(LocalDate hoy);

    boolean existenDelCliente(Integer idCliente);

    /** Mayor correlativo usado con el prefijo (p. ej. COT-2026-), bloqueando la serie. */
    int reservarCorrelativo(String prefijo);

    Cotizacion guardar(Cotizacion cotizacion);
}
