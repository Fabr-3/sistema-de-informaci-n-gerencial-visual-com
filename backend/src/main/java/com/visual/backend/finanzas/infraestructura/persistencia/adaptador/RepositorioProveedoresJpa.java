package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoDocumentoJpaRepositorio;
import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.finanzas.dominio.modelo.DocumentoProveedor;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.dominio.puerto.RepositorioProveedores;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.DocumentoProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.jpa.ProveedorJpaRepositorio;
import com.visual.backend.finanzas.infraestructura.persistencia.jpa.TipoProveedorJpaRepositorio;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

@Repository
public class RepositorioProveedoresJpa implements RepositorioProveedores {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("razonSocial", "id");

    private final ProveedorJpaRepositorio proveedores;
    private final TipoProveedorJpaRepositorio tiposProveedor;
    private final TipoDocumentoJpaRepositorio tiposDocumento;

    public RepositorioProveedoresJpa(ProveedorJpaRepositorio proveedores, TipoProveedorJpaRepositorio tiposProveedor,
                                     TipoDocumentoJpaRepositorio tiposDocumento) {
        this.proveedores = proveedores;
        this.tiposProveedor = tiposProveedor;
        this.tiposDocumento = tiposDocumento;
    }

    @Override
    public Optional<Proveedor> buscarPorId(Integer id) {
        return proveedores.findByIdAndFechaEliminacionIsNull(id).map(MapeadorFinanzas::aDominio);
    }

    @Override
    public Pagina<Proveedor> buscar(Filtros.Proveedores filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(proveedores.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorFinanzas::aDominio));
    }

    @Override
    public boolean existeDocumento(Integer idTipoDocumento, String numero, String complemento, Integer idExcluido) {
        return proveedores.existeDocumento(idTipoDocumento, numero, complemento == null ? "" : complemento,
                idExcluido);
    }

    @Override
    public List<ElementoCatalogo> tiposProveedor() {
        return tiposProveedor.findAll(Sort.by("nombre")).stream()
                .map(tipo -> new ElementoCatalogo(tipo.getId(), tipo.getNombre())).toList();
    }

    @Override
    public List<ElementoCatalogo> tiposDocumento() {
        return tiposDocumento.findAll(Sort.by("id")).stream()
                .map(tipo -> new ElementoCatalogo(tipo.getId(), tipo.getNombre())).toList();
    }

    @Override
    public boolean tieneMovimientos(Integer idProveedor) {
        return proveedores.tieneMovimientos(idProveedor);
    }

    /**
     * La lista de servicios solo se reemplaza si cambio: al reemplazarla se
     * pierde el vinculo de las subcontrataciones con el servicio elegido.
     */
    @Override
    public Proveedor guardar(Proveedor proveedor) {
        ProveedorEntidad entidad = proveedor.getId() == null ? new ProveedorEntidad()
                : proveedores.getReferenceById(proveedor.getId());
        entidad.setTipo(tiposProveedor.getReferenceById(proveedor.getTipo().id()));
        entidad.setRazonSocial(proveedor.getRazonSocial());
        entidad.setDireccion(proveedor.getDireccion());
        entidad.setActivo(proveedor.isActivo());
        entidad.setTelefono(proveedor.getTelefono());
        entidad.setEmail(proveedor.getEmail());
        entidad.setCreadoPor(proveedor.getCreadoPor());
        entidad.setFechaCreacion(proveedor.getFechaCreacion());
        entidad.setModificadoPor(proveedor.getModificadoPor());
        entidad.setFechaModificacion(proveedor.getFechaModificacion());
        entidad.setFechaEliminacion(proveedor.getFechaEliminacion());
        if (!entidad.getServicios().equals(proveedor.getServicios())) {
            entidad.getServicios().clear();
            entidad.getServicios().addAll(proveedor.getServicios());
        }
        sincronizarDocumentos(entidad, proveedor);
        return MapeadorFinanzas.aDominio(proveedores.saveAndFlush(entidad));
    }

    /** Conserva los documentos por id, actualiza los modificados, agrega los nuevos y quita los ausentes. */
    private void sincronizarDocumentos(ProveedorEntidad entidad, Proveedor proveedor) {
        Map<Integer, DocumentoProveedorEntidad> existentes = entidad.getDocumentos().stream()
                .collect(Collectors.toMap(DocumentoProveedorEntidad::getId, Function.identity()));
        List<DocumentoProveedorEntidad> resultado = new ArrayList<>();
        for (DocumentoProveedor documento : proveedor.getDocumentos()) {
            DocumentoProveedorEntidad fila = documento.id() == null ? null : existentes.get(documento.id());
            if (fila == null) {
                fila = new DocumentoProveedorEntidad();
                fila.setProveedor(entidad);
                fila.setCreadoPor(proveedor.getModificadoPor() == null ? proveedor.getCreadoPor()
                        : proveedor.getModificadoPor());
                fila.setFechaCreacion(proveedor.getFechaModificacion() == null ? proveedor.getFechaCreacion()
                        : proveedor.getFechaModificacion());
            } else if (!Objects.equals(fila.getNumero(), documento.numero())
                    || !Objects.equals(fila.getComplemento(), documento.complemento())
                    || !fila.getTipo().getId().equals(documento.tipo().id())) {
                fila.setModificadoPor(proveedor.getModificadoPor());
                fila.setFechaModificacion(proveedor.getFechaModificacion());
            }
            fila.setTipo(tiposDocumento.getReferenceById(documento.tipo().id()));
            fila.setNumero(documento.numero());
            fila.setComplemento(documento.complemento());
            resultado.add(fila);
        }
        entidad.getDocumentos().retainAll(resultado);
        resultado.stream().filter(fila -> !entidad.getDocumentos().contains(fila)).forEach(entidad.getDocumentos()::add);
    }

    /** Excluye eliminados; el texto busca en razon social, documentos y servicios. */
    private static Specification<ProveedorEntidad> especificacion(Filtros.Proveedores filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                Subquery<Integer> documentos = consulta.subquery(Integer.class);
                var documento = documentos.from(DocumentoProveedorEntidad.class);
                documentos.select(documento.get("proveedor").get("id")).where(
                        cb.like(cb.lower(documento.get("numero")), patron, ConsultasJpa.ESCAPE_LIKE));
                Subquery<Integer> servicios = consulta.subquery(Integer.class);
                var otro = servicios.from(ProveedorEntidad.class);
                var servicio = otro.join("servicios", JoinType.INNER);
                servicios.select(otro.get("id")).where(
                        cb.like(cb.lower(servicio.as(String.class)), patron, ConsultasJpa.ESCAPE_LIKE));
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("razonSocial")), patron, ConsultasJpa.ESCAPE_LIKE),
                        raiz.get("id").in(documentos),
                        raiz.get("id").in(servicios)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
