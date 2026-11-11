package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.finanzas.dominio.modelo.EstadoMovimiento;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.ItemVenta;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.TotalesMovimientos;
import com.visual.backend.finanzas.dominio.puerto.RepositorioMovimientos;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.MovimientoFinancieroEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.TipoMovimientoEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.VentaDetalleEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.jpa.MovimientoFinancieroJpaRepositorio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class RepositorioMovimientosJpa implements RepositorioMovimientos {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));

    private final MovimientoFinancieroJpaRepositorio movimientos;
    private final EntityManager entityManager;

    public RepositorioMovimientosJpa(MovimientoFinancieroJpaRepositorio movimientos, EntityManager entityManager) {
        this.movimientos = movimientos;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<MovimientoFinanciero> buscarPorId(Integer id) {
        return movimientos.findById(id).map(MapeadorFinanzas::aDominio);
    }

    @Override
    public Pagina<MovimientoFinanciero> buscar(Filtros.Movimientos filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(movimientos.findAll(
                (raiz, consulta, cb) -> cb.and(condiciones(raiz, cb, filtro, filtro.estado())),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorFinanzas::aDominio));
    }

    @Override
    public TotalesMovimientos totales(Filtros.Movimientos filtro) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> consulta = cb.createQuery(Object[].class);
        Root<MovimientoFinancieroEntidad> raiz = consulta.from(MovimientoFinancieroEntidad.class);
        consulta.multiselect(suma(raiz, cb, Naturaleza.INGRESO), suma(raiz, cb, Naturaleza.EGRESO))
                .where(condiciones(raiz, cb, filtro, EstadoMovimiento.REGISTRADO));
        Object[] fila = entityManager.createQuery(consulta).getSingleResult();
        return new TotalesMovimientos((BigDecimal) fila[0], (BigDecimal) fila[1]);
    }

    @Override
    public MovimientoFinanciero guardar(MovimientoFinanciero movimiento) {
        MovimientoFinancieroEntidad entidad = movimiento.getId() == null ? nueva(movimiento)
                : movimientos.getReferenceById(movimiento.getId());
        entidad.setEstado(movimiento.getEstado());
        entidad.setMotivoAnulacion(movimiento.getMotivoAnulacion());
        entidad.setModificadoPor(movimiento.getModificadoPor());
        entidad.setFechaModificacion(movimiento.getFechaModificacion());
        return MapeadorFinanzas.aDominio(movimientos.saveAndFlush(entidad));
    }

    /** Los datos del movimiento no cambian despues de registrarlo; solo su estado. */
    private MovimientoFinancieroEntidad nueva(MovimientoFinanciero movimiento) {
        MovimientoFinancieroEntidad entidad = new MovimientoFinancieroEntidad();
        entidad.setTipo(entityManager.getReference(TipoMovimientoEntidad.class, movimiento.getTipo().id()));
        entidad.setConcepto(movimiento.getConcepto());
        entidad.setMonto(movimiento.getMonto());
        entidad.setFecha(movimiento.getFecha());
        entidad.setProyecto(movimiento.getProyecto() == null ? null
                : entityManager.getReference(ProyectoEntidad.class, movimiento.getProyecto().id()));
        entidad.setEvento(movimiento.getEvento() == null ? null
                : entityManager.getReference(EventoEntidad.class, movimiento.getEvento().id()));
        entidad.setProveedor(movimiento.getProveedor() == null ? null
                : entityManager.getReference(ProveedorEntidad.class, movimiento.getProveedor().id()));
        entidad.setNumeroFactura(movimiento.getNumeroFactura());
        entidad.setIdMantenimiento(movimiento.getIdMantenimiento());
        entidad.setCreadoPor(movimiento.getCreadoPor());
        entidad.setFechaCreacion(movimiento.getFechaCreacion());
        for (ItemVenta item : movimiento.getItems()) {
            VentaDetalleEntidad detalle = new VentaDetalleEntidad();
            detalle.setMovimiento(entidad);
            detalle.setCantidad(item.cantidad());
            detalle.setActivo(item.esUnidad() ? entityManager.getReference(ActivoEntidad.class, item.idActivo()) : null);
            detalle.setExistencia(item.esUnidad() ? null
                    : entityManager.getReference(ExistenciaLoteEntidad.class, item.idExistencia()));
            entidad.getDetalle().add(detalle);
        }
        return entidad;
    }

    private static Expression<BigDecimal> suma(Root<MovimientoFinancieroEntidad> raiz, CriteriaBuilder cb,
                                               Naturaleza naturaleza) {
        Expression<BigDecimal> monto = cb.<BigDecimal>selectCase()
                .when(cb.equal(raiz.get("tipo").get("naturaleza"), naturaleza), raiz.<BigDecimal>get("monto"))
                .otherwise(BigDecimal.ZERO);
        return cb.coalesce(cb.sum(monto), BigDecimal.ZERO);
    }

    /** El estado se recibe aparte: los totales siempre consideran solo los registrados. */
    private static Predicate[] condiciones(Root<MovimientoFinancieroEntidad> raiz, CriteriaBuilder cb,
                                           Filtros.Movimientos filtro, EstadoMovimiento estado) {
        List<Predicate> condiciones = new ArrayList<>();
        if (estado != null) {
            condiciones.add(cb.equal(raiz.get("estado"), estado));
        }
        if (filtro.desde() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), filtro.desde()));
        }
        if (filtro.hasta() != null) {
            condiciones.add(cb.lessThanOrEqualTo(raiz.get("fecha"), filtro.hasta()));
        }
        if (filtro.naturaleza() != null) {
            condiciones.add(cb.equal(raiz.get("tipo").get("naturaleza"), filtro.naturaleza()));
        }
        agregarIgualdad(condiciones, cb, raiz.get("tipo").get("id"), filtro.idTipo());
        agregarIgualdad(condiciones, cb, raiz.get("proyecto").get("id"), filtro.idProyecto());
        agregarIgualdad(condiciones, cb, raiz.get("evento").get("id"), filtro.idEvento());
        agregarIgualdad(condiciones, cb, raiz.get("proveedor").get("id"), filtro.idProveedor());
        if (filtro.texto() != null && !filtro.texto().isBlank()) {
            String patron = ConsultasJpa.patronContiene(filtro.texto());
            condiciones.add(cb.or(
                    cb.like(cb.lower(raiz.get("concepto")), patron, ConsultasJpa.ESCAPE_LIKE),
                    cb.like(cb.lower(raiz.get("numeroFactura")), patron, ConsultasJpa.ESCAPE_LIKE)));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    private static void agregarIgualdad(List<Predicate> condiciones, CriteriaBuilder cb, Expression<?> campo,
                                        Integer valor) {
        if (valor != null) {
            condiciones.add(cb.equal(campo, valor));
        }
    }
}
