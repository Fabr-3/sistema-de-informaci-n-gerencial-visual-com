package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

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

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.ContactoCliente;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.puerto.RepositorioClientes;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ContactoClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.DocumentoClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ClienteJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoClienteJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoContactoJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoDocumentoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

@Repository
public class RepositorioClientesJpa implements RepositorioClientes {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("razonSocial", "id");

    private final ClienteJpaRepositorio clientes;
    private final TipoClienteJpaRepositorio tiposCliente;
    private final TipoDocumentoJpaRepositorio tiposDocumento;
    private final TipoContactoJpaRepositorio tiposContacto;

    public RepositorioClientesJpa(ClienteJpaRepositorio clientes, TipoClienteJpaRepositorio tiposCliente,
                                  TipoDocumentoJpaRepositorio tiposDocumento,
                                  TipoContactoJpaRepositorio tiposContacto) {
        this.clientes = clientes;
        this.tiposCliente = tiposCliente;
        this.tiposDocumento = tiposDocumento;
        this.tiposContacto = tiposContacto;
    }

    @Override
    public Optional<Cliente> buscarPorId(Integer id) {
        return clientes.findByIdAndFechaEliminacionIsNull(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public Pagina<Cliente> buscar(Filtros.Clientes filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(clientes.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorComercial::aDominio));
    }

    @Override
    public boolean existeDocumento(Integer idTipoDocumento, String numero, Integer idExcluido) {
        return clientes.existeDocumento(idTipoDocumento, numero, idExcluido);
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteEntidad entidad = cliente.getId() == null ? new ClienteEntidad()
                : clientes.getReferenceById(cliente.getId());
        entidad.setTipo(tiposCliente.getReferenceById(cliente.getTipo().id()));
        entidad.setRazonSocial(cliente.getRazonSocial());
        entidad.setDireccion(cliente.getDireccion());
        entidad.setCreadoPor(cliente.getCreadoPor());
        entidad.setFechaCreacion(cliente.getFechaCreacion());
        entidad.setModificadoPor(cliente.getModificadoPor());
        entidad.setFechaModificacion(cliente.getFechaModificacion());
        entidad.setFechaEliminacion(cliente.getFechaEliminacion());
        sincronizarDocumentos(entidad, cliente);
        sincronizarContactos(entidad, cliente.getContactos());
        return MapeadorComercial.aDominio(clientes.save(entidad));
    }

    /**
     * Actualiza en el lugar los documentos que conservan su id, quita los
     * ausentes y agrega los nuevos (evita reinsertar documentos unicos).
     */
    private void sincronizarDocumentos(ClienteEntidad entidad, Cliente cliente) {
        Map<Integer, DocumentoClienteEntidad> existentes = porId(entidad.getDocumentos(), DocumentoClienteEntidad::getId);
        List<DocumentoClienteEntidad> resultado = new ArrayList<>();
        for (DocumentoCliente documento : cliente.getDocumentos()) {
            DocumentoClienteEntidad fila = documento.id() != null ? existentes.get(documento.id())
                    : buscarPorClave(entidad.getDocumentos(), existente -> existente.getTipo().getId().equals(documento.tipo().id())
                            && existente.getNumero().equalsIgnoreCase(documento.numero()));
            if (fila == null) {
                fila = new DocumentoClienteEntidad();
                fila.setCliente(entidad);
                fila.setCreadoPor(cliente.getModificadoPor() == null ? cliente.getCreadoPor() : cliente.getModificadoPor());
                fila.setFechaCreacion(cliente.getFechaModificacion() == null ? cliente.getFechaCreacion()
                        : cliente.getFechaModificacion());
            } else if (!Objects.equals(fila.getNumero(), documento.numero())
                    || !fila.getTipo().getId().equals(documento.tipo().id())) {
                fila.setModificadoPor(cliente.getModificadoPor());
                fila.setFechaModificacion(cliente.getFechaModificacion());
            }
            fila.setTipo(tiposDocumento.getReferenceById(documento.tipo().id()));
            fila.setNumero(documento.numero());
            resultado.add(fila);
        }
        entidad.getDocumentos().retainAll(resultado);
        resultado.stream().filter(fila -> !entidad.getDocumentos().contains(fila)).forEach(entidad.getDocumentos()::add);
    }

    private void sincronizarContactos(ClienteEntidad entidad, List<ContactoCliente> contactos) {
        Map<Integer, ContactoClienteEntidad> existentes = porId(entidad.getContactos(), ContactoClienteEntidad::getId);
        List<ContactoClienteEntidad> resultado = new ArrayList<>();
        for (ContactoCliente contacto : contactos) {
            ContactoClienteEntidad fila = contacto.id() != null ? existentes.get(contacto.id())
                    : buscarPorClave(entidad.getContactos(), existente -> existente.getTipo().getId().equals(contacto.tipo().id())
                            && existente.getValor().equalsIgnoreCase(contacto.valor()));
            if (fila == null) {
                fila = new ContactoClienteEntidad();
                fila.setCliente(entidad);
            }
            fila.setTipo(tiposContacto.getReferenceById(contacto.tipo().id()));
            fila.setValor(contacto.valor());
            fila.setReferencia(contacto.referencia());
            resultado.add(fila);
        }
        entidad.getContactos().retainAll(resultado);
        resultado.stream().filter(fila -> !entidad.getContactos().contains(fila)).forEach(entidad.getContactos()::add);
    }

    /**
     * Fila existente con la misma clave natural: un elemento enviado sin id
     * que ya existe se actualiza en lugar de reinsertarse (evita violar la
     * unicidad, porque Hibernate inserta antes de borrar).
     */
    private static <T> T buscarPorClave(List<T> filas, java.util.function.Predicate<T> mismaClave) {
        return filas.stream().filter(mismaClave).findFirst().orElse(null);
    }

    private static <T> Map<Integer, T> porId(List<T> filas, Function<T, Integer> id) {
        return filas.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    /** Excluye eliminados; el texto busca en la razon social y en los numeros de documento. */
    private static Specification<ClienteEntidad> especificacion(Filtros.Clientes filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                Subquery<Integer> documentos = consulta.subquery(Integer.class);
                var documento = documentos.from(DocumentoClienteEntidad.class);
                documentos.select(documento.get("cliente").get("id")).where(
                        cb.like(cb.lower(documento.get("numero")), patron, ConsultasJpa.ESCAPE_LIKE));
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("razonSocial")), patron, ConsultasJpa.ESCAPE_LIKE),
                        raiz.get("id").in(documentos)));
            }
            if (filtro.idTipoCliente() != null) {
                condiciones.add(cb.equal(raiz.get("tipo").get("id"), filtro.idTipoCliente()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
