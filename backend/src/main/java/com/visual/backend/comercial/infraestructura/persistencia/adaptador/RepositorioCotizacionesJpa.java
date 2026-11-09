package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.infraestructura.persistencia.jpa.UsuarioJpaRepositorio;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.puerto.RepositorioCotizaciones;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.CotizacionEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.LineaCotizacionEmbebida;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ClienteJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.CotizacionJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ServicioJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.compartido.infraestructura.persistencia.GeneradorCorrelativos;

import jakarta.persistence.criteria.Predicate;

@Repository
public class RepositorioCotizacionesJpa implements RepositorioCotizaciones {

    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaEmision"), Sort.Order.desc("id"));
    private static final String TABLA = "cotizaciones";
    private static final String COLUMNA_NUMERO = "numero";

    private final CotizacionJpaRepositorio cotizaciones;
    private final ClienteJpaRepositorio clientes;
    private final ServicioJpaRepositorio servicios;
    private final GeneradorCorrelativos correlativos;
    private final UsuarioJpaRepositorio usuarios;

    public RepositorioCotizacionesJpa(CotizacionJpaRepositorio cotizaciones, ClienteJpaRepositorio clientes,
                                      ServicioJpaRepositorio servicios, GeneradorCorrelativos correlativos,
                                      UsuarioJpaRepositorio usuarios) {
        this.cotizaciones = cotizaciones;
        this.clientes = clientes;
        this.servicios = servicios;
        this.correlativos = correlativos;
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Cotizacion> buscarPorId(Integer id) {
        return cotizaciones.findByIdAndFechaEliminacionIsNull(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public Pagina<Cotizacion> buscar(Filtros.Cotizaciones filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(cotizaciones.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, RECIENTES_PRIMERO)).map(MapeadorComercial::aDominio));
    }

    @Override
    public List<Cotizacion> buscarBorradoresCaducados(LocalDate hoy) {
        return cotizaciones.findByEstadoAndFechaValidezBeforeAndFechaEliminacionIsNull(EstadoCotizacion.BORRADOR, hoy)
                .stream().map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public boolean existenDelCliente(Integer idCliente) {
        return cotizaciones.existsByClienteId(idCliente);
    }

    @Override
    public int reservarCorrelativo(String prefijo) {
        return correlativos.reservarUltimo(TABLA, COLUMNA_NUMERO, prefijo);
    }

    /**
     * Las lineas solo se reescriben en borrador: un trigger impide tocar las
     * de una cotizacion resuelta. Se guarda con flush inmediato para que las
     * lineas se escriban mientras la cotizacion sigue en borrador, aunque
     * otra operacion de la misma transaccion la resuelva despues.
     */
    @Override
    public Cotizacion guardar(Cotizacion cotizacion) {
        CotizacionEntidad entidad = cotizacion.getId() == null ? new CotizacionEntidad()
                : cotizaciones.getReferenceById(cotizacion.getId());
        entidad.setNumero(cotizacion.getNumero());
        entidad.setCliente(clientes.getReferenceById(cotizacion.getCliente().id()));
        entidad.setTitulo(cotizacion.getTitulo());
        entidad.setFechaEmision(cotizacion.getFechaEmision());
        entidad.setFechaValidez(cotizacion.getFechaValidez());
        entidad.setFechaEventoInicio(cotizacion.getFechaEventoInicio());
        entidad.setFechaEventoFin(cotizacion.getFechaEventoFin());
        entidad.setEstado(cotizacion.getEstado());
        entidad.setCondiciones(cotizacion.getCondiciones());
        entidad.setResueltoPor(cotizacion.getResueltoPor());
        // Relacion de solo lectura: se asigna para devolver el nombre de quien resolvio
        entidad.setResolutor(cotizacion.getResueltoPor() == null ? null
                : usuarios.getReferenceById(cotizacion.getResueltoPor()));
        entidad.setFechaResolucion(cotizacion.getFechaResolucion());
        entidad.setMotivoRechazo(cotizacion.getMotivoRechazo());
        entidad.setCreadoPor(cotizacion.getCreadoPor());
        entidad.setFechaCreacion(cotizacion.getFechaCreacion());
        entidad.setModificadoPor(cotizacion.getModificadoPor());
        entidad.setFechaModificacion(cotizacion.getFechaModificacion());
        entidad.setFechaEliminacion(cotizacion.getFechaEliminacion());
        if (cotizacion.esBorrador() && cotizacion.getFechaEliminacion() == null) {
            entidad.getLineas().clear();
            cotizacion.getLineas().forEach(linea -> entidad.getLineas().add(new LineaCotizacionEmbebida(
                    servicios.getReferenceById(linea.idServicio()), linea.cantidad(), linea.precioUnitario(),
                    linea.descuento())));
        }
        return MapeadorComercial.aDominio(cotizaciones.saveAndFlush(entidad));
    }

    private static Specification<CotizacionEntidad> especificacion(Filtros.Cotizaciones filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("numero")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(raiz.get("titulo")), patron, ConsultasJpa.ESCAPE_LIKE),
                        cb.like(cb.lower(raiz.get("cliente").get("razonSocial")), patron, ConsultasJpa.ESCAPE_LIKE)));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            if (filtro.idCliente() != null) {
                condiciones.add(cb.equal(raiz.get("cliente").get("id"), filtro.idCliente()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
