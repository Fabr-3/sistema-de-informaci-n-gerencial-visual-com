package com.visual.backend.comercial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoCliente;
import com.visual.backend.comercial.aplicacion.dto.VistaCliente;
import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.ContactoCliente;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.puerto.RepositorioCatalogosComerciales;
import com.visual.backend.comercial.dominio.puerto.RepositorioClientes;
import com.visual.backend.comercial.dominio.puerto.RepositorioCotizaciones;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Registro de clientes con sus documentos y contactos. */
@Service
public class ServicioClientes {

    public static final String ENTIDAD = "clientes";

    private final RepositorioClientes repositorio;
    private final RepositorioCatalogosComerciales catalogos;
    private final RepositorioCotizaciones cotizaciones;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioClientes(RepositorioClientes repositorio, RepositorioCatalogosComerciales catalogos,
                            RepositorioCotizaciones cotizaciones, ServicioAuditoria auditoria,
                            ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.catalogos = catalogos;
        this.cotizaciones = cotizaciones;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaCliente> listar(Filtros.Clientes filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud).mapear(VistaCliente::de);
    }

    @Transactional(readOnly = true)
    public VistaCliente obtener(Integer id) {
        return VistaCliente.de(repositorio.obtener(id));
    }

    @Transactional
    public VistaCliente crear(ComandoCliente comando) {
        List<DocumentoCliente> documentos = documentos(comando, null);
        Cliente cliente = Cliente.nuevo(tipoCliente(comando), comando.razonSocial(), comando.direccion(), documentos,
                contactos(comando), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaCliente creado = VistaCliente.de(repositorio.guardar(cliente));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null, creado.comoMapaAuditoria(),
                null);
        return creado;
    }

    @Transactional
    public VistaCliente actualizar(Integer id, ComandoCliente comando) {
        Cliente cliente = repositorio.obtener(id);
        VistaCliente anterior = VistaCliente.de(cliente);
        cliente.actualizar(tipoCliente(comando), comando.razonSocial(), comando.direccion(), documentos(comando, id),
                contactos(comando), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaCliente actualizado = VistaCliente.de(repositorio.guardar(cliente));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    /** Un cliente con cotizaciones se conserva por su historial comercial. */
    @Transactional
    public void eliminar(Integer id) {
        Cliente cliente = repositorio.obtener(id);
        if (cotizaciones.existenDelCliente(id)) {
            throw new ReglaNegocioExcepcion("El cliente tiene cotizaciones registradas y no se puede eliminar");
        }
        VistaCliente anterior = VistaCliente.de(cliente);
        cliente.eliminar(contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        repositorio.guardar(cliente);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, id, anterior.comoMapaAuditoria(), null, null);
    }

    private ElementoCatalogo tipoCliente(ComandoCliente comando) {
        return catalogos.buscarTipoCliente(comando.idTipoCliente())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de cliente", comando.idTipoCliente()));
    }

    /** Un mismo documento (tipo y numero) no puede pertenecer a dos clientes. */
    private List<DocumentoCliente> documentos(ComandoCliente comando, Integer idCliente) {
        return comando.documentos().stream().map(documento -> {
            ElementoCatalogo tipo = catalogos.buscarTipoDocumento(documento.idTipoDocumento())
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de documento",
                            documento.idTipoDocumento()));
            String numero = documento.numero().trim();
            if (repositorio.existeDocumento(tipo.id(), numero, idCliente)) {
                throw new DatoDuplicadoExcepcion("El " + tipo.nombre() + " " + numero
                        + " ya está registrado en otro cliente");
            }
            return new DocumentoCliente(documento.id(), tipo, numero);
        }).toList();
    }

    private List<ContactoCliente> contactos(ComandoCliente comando) {
        if (comando.contactos() == null) {
            return List.of();
        }
        return comando.contactos().stream().map(contacto -> new ContactoCliente(contacto.id(),
                catalogos.buscarTipoContacto(contacto.idTipoContacto())
                        .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de contacto",
                                contacto.idTipoContacto())),
                contacto.valor(), contacto.referencia())).toList();
    }
}
