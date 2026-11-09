package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.FiltroActivos;
import com.visual.backend.activos.dominio.modelo.UsoActivo;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.UsoActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ActivoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.BodegaJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.MantenimientoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ModeloActivoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ModuloLedJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.RegistroUsoJpaRepositorio;
import com.visual.backend.activos.infraestructura.persistencia.jpa.UsoActivoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.compartido.infraestructura.persistencia.GeneradorCorrelativos;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Repository
public class RepositorioActivosJpa implements RepositorioActivos {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("codigoInventario");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int ESCALA_FACTOR = 4;
    private static final String TABLA_ACTIVOS = "activos";
    private static final String COLUMNA_CODIGO = "codigo_inventario";
    private static final String SEPARADOR_CODIGO = "-";

    private final ActivoJpaRepositorio activos;
    private final ModeloActivoJpaRepositorio modelos;
    private final BodegaJpaRepositorio bodegas;
    private final UsoActivoJpaRepositorio usos;
    private final RegistroUsoJpaRepositorio registrosUso;
    private final MantenimientoJpaRepositorio mantenimientos;
    private final ModuloLedJpaRepositorio modulos;
    private final GeneradorCorrelativos correlativos;

    public RepositorioActivosJpa(ActivoJpaRepositorio activos, ModeloActivoJpaRepositorio modelos,
                                 BodegaJpaRepositorio bodegas, UsoActivoJpaRepositorio usos,
                                 RegistroUsoJpaRepositorio registrosUso, MantenimientoJpaRepositorio mantenimientos,
                                 ModuloLedJpaRepositorio modulos, GeneradorCorrelativos correlativos) {
        this.activos = activos;
        this.modelos = modelos;
        this.bodegas = bodegas;
        this.usos = usos;
        this.registrosUso = registrosUso;
        this.mantenimientos = mantenimientos;
        this.modulos = modulos;
        this.correlativos = correlativos;
    }

    @Override
    public Optional<Activo> buscarPorId(Integer id) {
        return activos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigoInventario) {
        return activos.existsByCodigoInventario(codigoInventario);
    }

    @Override
    public boolean existeSerie(Integer idModelo, String numeroSerie, Integer idExcluido) {
        return idExcluido == null ? activos.existsByModeloIdAndNumeroSerieIgnoreCase(idModelo, numeroSerie)
                : activos.existsByModeloIdAndNumeroSerieIgnoreCaseAndIdNot(idModelo, numeroSerie, idExcluido);
    }

    @Override
    public int reservarCorrelativo(String prefijoCodigo) {
        return correlativos.reservarUltimo(TABLA_ACTIVOS, COLUMNA_CODIGO, prefijoCodigo + SEPARADOR_CODIGO);
    }

    @Override
    public Pagina<Activo> buscar(FiltroActivos filtro, int porcentajeAlerta, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(activos.findAll(especificacion(filtro, porcentajeAlerta),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorActivos::aDominio));
    }

    @Override
    public List<Activo> buscarConAlertaMantenimiento(int porcentajeAlerta) {
        FiltroActivos filtro = new FiltroActivos(null, null, null, null, null, true);
        return activos.findAll(especificacion(filtro, porcentajeAlerta)).stream()
                .map(MapeadorActivos::aDominio)
                .toList();
    }

    @Override
    public UsoActivo obtenerUso(Integer idActivo) {
        return obtenerUso(List.of(idActivo)).getOrDefault(idActivo, UsoActivo.SIN_USO);
    }

    @Override
    public Map<Integer, UsoActivo> obtenerUso(List<Integer> idsActivos) {
        return usos.findByIdActivoIn(idsActivos).stream()
                .collect(Collectors.toMap(UsoActivoJpaRepositorio.ProyeccionUso::getIdActivo,
                        MapeadorActivos::aDominio));
    }

    @Override
    public boolean tieneHistorial(Integer idActivo) {
        return registrosUso.existsByIdActivo(idActivo) || mantenimientos.existsByIdActivo(idActivo)
                || modulos.existsByIdActivoPadre(idActivo);
    }

    @Override
    public Activo guardar(Activo activo) {
        ActivoEntidad entidad = activo.getId() == null ? new ActivoEntidad()
                : activos.getReferenceById(activo.getId());
        entidad.setCodigoInventario(activo.getCodigoInventario());
        entidad.setModelo(modelos.getReferenceById(activo.getModelo().id()));
        entidad.setNumeroSerie(activo.getNumeroSerie());
        entidad.setEstado(activo.getEstado());
        entidad.setBodega(bodegas.getReferenceById(activo.getBodega().id()));
        entidad.setFechaAdquisicion(activo.getFechaAdquisicion());
        entidad.setCostoAdquisicion(activo.getCostoAdquisicion());
        entidad.setObservaciones(activo.getObservaciones());
        entidad.setMotivoBaja(activo.getMotivoBaja());
        entidad.setCreadoPor(activo.getCreadoPor());
        entidad.setModificadoPor(activo.getModificadoPor());
        entidad.setFechaCreacion(activo.getFechaCreacion());
        entidad.setFechaModificacion(activo.getFechaModificacion());
        entidad.setFechaEliminacion(activo.getFechaEliminacion());
        return MapeadorActivos.aDominio(activos.save(entidad));
    }

    private static Specification<ActivoEntidad> especificacion(FiltroActivos filtro, int porcentajeAlerta) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            Path<Object> modelo = raiz.get("modelo");
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                Function<Path<String>, Predicate> contiene =
                        campo -> cb.like(cb.lower(campo), patron, ConsultasJpa.ESCAPE_LIKE);
                condiciones.add(cb.or(
                        contiene.apply(raiz.get("codigoInventario")),
                        contiene.apply(raiz.get("numeroSerie")),
                        contiene.apply(modelo.get("nombre")),
                        contiene.apply(modelo.get("fabricante").get("nombre"))));
            }
            if (filtro.idCategoria() != null) {
                condiciones.add(cb.equal(modelo.get("categoria").get("id"), filtro.idCategoria()));
            }
            if (filtro.idModelo() != null) {
                condiciones.add(cb.equal(modelo.get("id"), filtro.idModelo()));
            }
            if (filtro.idBodega() != null) {
                condiciones.add(cb.equal(raiz.get("bodega").get("id"), filtro.idBodega()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            if (filtro.soloAlertas()) {
                condiciones.add(cb.notEqual(raiz.get("estado"), EstadoOperativo.BAJA));
                condiciones.add(alcanzoUmbral(raiz, consulta.subquery(Integer.class), cb, porcentajeAlerta));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /**
     * Subconsulta correlacionada: horas desde el ultimo mantenimiento >=
     * intervalo del modelo * porcentaje de alerta.
     */
    private static Predicate alcanzoUmbral(Root<ActivoEntidad> raiz, Subquery<Integer> subconsulta,
                                           CriteriaBuilder cb, int porcentajeAlerta) {
        BigDecimal factor = BigDecimal.valueOf(porcentajeAlerta).divide(CIEN, ESCALA_FACTOR, RoundingMode.HALF_UP);
        Root<UsoActivoEntidad> uso = subconsulta.from(UsoActivoEntidad.class);
        Path<Integer> intervalo = raiz.get("modelo").get("horasMantenimiento");
        subconsulta.select(uso.get("idActivo")).where(
                cb.equal(uso.get("idActivo"), raiz.get("id")),
                cb.isNotNull(intervalo),
                cb.ge(uso.get("horasDesdeMantenimiento"), cb.prod(intervalo, factor)));
        return cb.exists(subconsulta);
    }
}
