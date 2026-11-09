package com.visual.backend.accesos.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.FiltroAuditoria;
import com.visual.backend.accesos.dominio.modelo.RegistroAuditoria;
import com.visual.backend.accesos.dominio.puerto.RepositorioAuditoria;
import com.visual.backend.accesos.infraestructura.persistencia.entidad.AuditoriaEntidad;
import com.visual.backend.accesos.infraestructura.persistencia.jpa.AuditoriaJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.persistence.criteria.Predicate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Adaptador de auditoria: los valores se guardan como JSONB. */
@Repository
public class RepositorioAuditoriaJpa implements RepositorioAuditoria {

    private static final Sort ORDEN_RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
    private static final TypeReference<Map<String, Object>> TIPO_MAPA = new TypeReference<>() {
    };

    private final AuditoriaJpaRepositorio auditoria;
    private final JsonMapper json;

    public RepositorioAuditoriaJpa(AuditoriaJpaRepositorio auditoria, JsonMapper json) {
        this.auditoria = auditoria;
        this.json = json;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        AuditoriaEntidad entidad = new AuditoriaEntidad();
        entidad.setFecha(registro.fecha());
        entidad.setIdUsuario(registro.idUsuario());
        entidad.setNombreUsuario(registro.nombreUsuario());
        entidad.setAccion(registro.accion());
        entidad.setEntidad(registro.entidad());
        entidad.setIdEntidad(registro.idEntidad());
        entidad.setValoresAnteriores(aJson(registro.valoresAnteriores()));
        entidad.setValoresNuevos(aJson(registro.valoresNuevos()));
        entidad.setDetalle(registro.detalle());
        entidad.setDireccionIp(registro.direccionIp());
        entidad.setAgenteUsuario(registro.agenteUsuario());
        auditoria.save(entidad);
    }

    @Override
    public Pagina<RegistroAuditoria> buscar(FiltroAuditoria filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(auditoria.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_RECIENTES_PRIMERO)).map(this::aDominio));
    }

    private RegistroAuditoria aDominio(AuditoriaEntidad entidad) {
        return new RegistroAuditoria(entidad.getId(), entidad.getFecha(), entidad.getIdUsuario(),
                entidad.getNombreUsuario(), entidad.getAccion(), entidad.getEntidad(), entidad.getIdEntidad(),
                aMapa(entidad.getValoresAnteriores()), aMapa(entidad.getValoresNuevos()), entidad.getDetalle(),
                entidad.getDireccionIp(), entidad.getAgenteUsuario());
    }

    private String aJson(Map<String, Object> valores) {
        return valores == null ? null : json.writeValueAsString(valores);
    }

    private Map<String, Object> aMapa(String valores) {
        return valores == null ? null : json.readValue(valores, TIPO_MAPA);
    }

    private static Specification<AuditoriaEntidad> especificacion(FiltroAuditoria filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(raiz.get("fecha"), filtro.hasta()));
            }
            if (filtro.idUsuario() != null) {
                condiciones.add(cb.equal(raiz.get("idUsuario"), filtro.idUsuario()));
            }
            if (filtro.accion() != null) {
                condiciones.add(cb.equal(raiz.get("accion"), filtro.accion()));
            }
            if (filtro.entidad() != null && !filtro.entidad().isBlank()) {
                condiciones.add(cb.equal(raiz.get("entidad"), filtro.entidad().trim()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
