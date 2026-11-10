package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.dominio.puerto.RepositorioProveedores;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.DocumentoProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.jpa.ProveedorJpaRepositorio;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

@Repository
public class RepositorioProveedoresJpa implements RepositorioProveedores {

    /** Tipo de documento (V1) con el que se guarda el NIT del proveedor. */
    static final String TIPO_NIT = "NIT";
    private static final Sort ORDEN_POR_DEFECTO = Sort.by("razonSocial", "id");

    private final ProveedorJpaRepositorio proveedores;

    public RepositorioProveedoresJpa(ProveedorJpaRepositorio proveedores) {
        this.proveedores = proveedores;
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
    public boolean existeNit(String nit, Integer idExcluido) {
        return proveedores.existeDocumento(TIPO_NIT, nit, idExcluido);
    }

    @Override
    public boolean tieneMovimientos(Integer idProveedor) {
        return proveedores.tieneMovimientos(idProveedor);
    }

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        ProveedorEntidad entidad = proveedor.getId() == null ? new ProveedorEntidad()
                : proveedores.getReferenceById(proveedor.getId());
        entidad.setRazonSocial(proveedor.getRazonSocial());
        entidad.setTelefono(proveedor.getTelefono());
        entidad.setEmail(proveedor.getEmail());
        entidad.setCreadoPor(proveedor.getCreadoPor());
        entidad.setFechaCreacion(proveedor.getFechaCreacion());
        entidad.setModificadoPor(proveedor.getModificadoPor());
        entidad.setFechaModificacion(proveedor.getFechaModificacion());
        entidad.setFechaEliminacion(proveedor.getFechaEliminacion());
        entidad.getServicios().clear();
        entidad.getServicios().addAll(proveedor.getServicios());
        sincronizarNit(entidad, proveedor);
        return MapeadorFinanzas.aDominio(proveedores.saveAndFlush(entidad));
    }

    /** El NIT se guarda como documento de tipo NIT; se corrige en el lugar o se quita. */
    private void sincronizarNit(ProveedorEntidad entidad, Proveedor proveedor) {
        DocumentoProveedorEntidad actual = MapeadorFinanzas.documentoNit(entidad);
        if (proveedor.getNit() == null) {
            if (actual != null) {
                entidad.getDocumentos().remove(actual);
            }
            return;
        }
        if (actual == null) {
            actual = new DocumentoProveedorEntidad();
            actual.setProveedor(entidad);
            actual.setTipo(proveedores.buscarTipoDocumento(TIPO_NIT)
                    .orElseThrow(() -> new IllegalStateException("Falta el tipo de documento NIT")));
            actual.setCreadoPor(proveedor.getModificadoPor() == null ? proveedor.getCreadoPor()
                    : proveedor.getModificadoPor());
            actual.setFechaCreacion(proveedor.getFechaModificacion() == null ? proveedor.getFechaCreacion()
                    : proveedor.getFechaModificacion());
            entidad.getDocumentos().add(actual);
        } else if (!actual.getNumero().equals(proveedor.getNit())) {
            actual.setModificadoPor(proveedor.getModificadoPor());
            actual.setFechaModificacion(proveedor.getFechaModificacion());
        }
        actual.setNumero(proveedor.getNit());
    }

    /** Excluye eliminados; el texto busca en razon social, NIT y servicios. */
    private static Specification<ProveedorEntidad> especificacion(Filtros.Proveedores filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
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
