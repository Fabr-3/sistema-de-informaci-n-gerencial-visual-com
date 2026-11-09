package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.ExistenciaLote;
import com.visual.backend.activos.dominio.modelo.FiltroExistencias;
import com.visual.backend.activos.dominio.modelo.MovimientoLote;
import com.visual.backend.activos.dominio.puerto.RepositorioLotes;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.MovimientoLoteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.BodegaJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ExistenciaLoteJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ModeloActivoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.MovimientoLoteJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

@Repository
public class RepositorioLotesJpa implements RepositorioLotes {

    private static final Sort ORDEN_EXISTENCIAS = Sort.by("modelo.fabricante.nombre", "modelo.nombre", "bodega.nombre");
    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));

    private final ExistenciaLoteJpaRepositorio existencias;
    private final MovimientoLoteJpaRepositorio movimientos;
    private final ModeloActivoJpaRepositorio modelos;
    private final BodegaJpaRepositorio bodegas;

    public RepositorioLotesJpa(ExistenciaLoteJpaRepositorio existencias, MovimientoLoteJpaRepositorio movimientos,
                               ModeloActivoJpaRepositorio modelos, BodegaJpaRepositorio bodegas) {
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.modelos = modelos;
        this.bodegas = bodegas;
    }

    @Override
    public List<ExistenciaLote> buscar(FiltroExistencias filtro) {
        return existencias.findAll(especificacion(filtro), ORDEN_EXISTENCIAS).stream()
                .map(MapeadorActivos::aDominio)
                .toList();
    }

    @Override
    public Optional<ExistenciaLote> buscarPorId(Integer id) {
        return existencias.findById(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public Optional<ExistenciaLote> buscarParaActualizar(Integer idModelo, Integer idBodega) {
        return existencias.findByModeloIdAndBodegaId(idModelo, idBodega).map(MapeadorActivos::aDominio);
    }

    @Override
    public ExistenciaLote guardar(ExistenciaLote existencia) {
        ExistenciaLoteEntidad entidad;
        if (existencia.getId() == null) {
            entidad = new ExistenciaLoteEntidad();
            entidad.setModelo(modelos.getReferenceById(existencia.getModelo().id()));
            entidad.setBodega(bodegas.getReferenceById(existencia.getBodega().id()));
        } else {
            entidad = existencias.getReferenceById(existencia.getId());
        }
        entidad.setCantidad(existencia.getCantidad());
        return MapeadorActivos.aDominio(existencias.save(entidad));
    }

    @Override
    public MovimientoLote registrarMovimiento(MovimientoLote movimiento) {
        MovimientoLoteEntidad entidad = new MovimientoLoteEntidad();
        entidad.setIdExistencia(movimiento.idExistencia());
        entidad.setTipo(movimiento.tipo());
        entidad.setCantidad(movimiento.cantidad());
        entidad.setMotivo(movimiento.motivo());
        entidad.setFecha(movimiento.fecha());
        entidad.setCreadoPor(movimiento.creadoPor());
        return MapeadorActivos.aDominio(movimientos.save(entidad));
    }

    @Override
    public Pagina<MovimientoLote> listarMovimientos(Integer idExistencia, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(movimientos.findByIdExistencia(idExistencia,
                ConsultasJpa.paginacion(solicitud, RECIENTES_PRIMERO)).map(MapeadorActivos::aDominio));
    }

    private static Specification<ExistenciaLoteEntidad> especificacion(FiltroExistencias filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            Path<Object> modelo = raiz.get("modelo");
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                condiciones.add(cb.or(
                        cb.like(cb.lower(modelo.get("nombre")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(modelo.get("fabricante").get("nombre")), patron, ConsultasJpa.ESCAPE_LIKE)));
            }
            if (filtro.idCategoria() != null) {
                condiciones.add(cb.equal(modelo.get("categoria").get("id"), filtro.idCategoria()));
            }
            if (filtro.idBodega() != null) {
                condiciones.add(cb.equal(raiz.get("bodega").get("id"), filtro.idBodega()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
