package com.visual.backend.finanzas.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.aplicacion.dto.CatalogosProveedores;
import com.visual.backend.finanzas.aplicacion.dto.ComandoProveedor;
import com.visual.backend.finanzas.aplicacion.dto.VistaProveedor;
import com.visual.backend.finanzas.dominio.modelo.DatosProveedor;
import com.visual.backend.finanzas.dominio.modelo.DocumentoProveedor;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.dominio.puerto.RepositorioProveedores;

/** Proveedores de compras, alquileres y servicios externos, con sus documentos y su estado. */
@Service
public class ServicioProveedores {

    public static final String ENTIDAD = "proveedores";
    static final String TIPO_POR_DEFECTO = "Empresa privada";

    private final RepositorioProveedores repositorio;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioProveedores(RepositorioProveedores repositorio, ServicioAuditoria auditoria,
                               ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaProveedor> listar(Filtros.Proveedores filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud).mapear(VistaProveedor::de);
    }

    @Transactional(readOnly = true)
    public VistaProveedor obtener(Integer id) {
        return VistaProveedor.de(repositorio.obtener(id));
    }

    @Transactional(readOnly = true)
    public CatalogosProveedores catalogos() {
        return new CatalogosProveedores(repositorio.tiposProveedor(), repositorio.tiposDocumento());
    }

    @Transactional
    public VistaProveedor crear(ComandoProveedor comando) {
        VistaProveedor creado = VistaProveedor.de(repositorio.guardar(Proveedor.nuevo(datos(comando, null),
                contexto.idUsuarioActual(), LocalDateTime.now(reloj))));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null, creado.comoMapaAuditoria(),
                null);
        return creado;
    }

    @Transactional
    public VistaProveedor actualizar(Integer id, ComandoProveedor comando) {
        Proveedor proveedor = repositorio.obtener(id);
        VistaProveedor anterior = VistaProveedor.de(proveedor);
        proveedor.actualizar(datos(comando, id), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaProveedor actualizado = VistaProveedor.de(repositorio.guardar(proveedor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    /** Anula o reactiva al proveedor: anulado no se elige en movimientos ni subcontrataciones nuevos. */
    @Transactional
    public VistaProveedor cambiarActivo(Integer id, boolean activo) {
        Proveedor proveedor = repositorio.obtener(id);
        VistaProveedor anterior = VistaProveedor.de(proveedor);
        proveedor.cambiarActivo(activo, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaProveedor actualizado = VistaProveedor.de(repositorio.guardar(proveedor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), activo ? "Proveedor activado" : "Proveedor anulado");
        return actualizado;
    }

    /** Un proveedor con movimientos se conserva por el historial financiero. */
    @Transactional
    public void eliminar(Integer id) {
        Proveedor proveedor = repositorio.obtener(id);
        if (repositorio.tieneMovimientos(id)) {
            throw new ReglaNegocioExcepcion("El proveedor tiene movimientos registrados y no se puede eliminar");
        }
        VistaProveedor anterior = VistaProveedor.de(proveedor);
        proveedor.eliminar(contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        repositorio.guardar(proveedor);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, id, anterior.comoMapaAuditoria(), null, null);
    }

    private DatosProveedor datos(ComandoProveedor comando, Integer idProveedor) {
        List<ElementoCatalogo> tipos = repositorio.tiposProveedor();
        ElementoCatalogo tipo = comando.idTipoProveedor() == null
                ? tipos.stream().filter(candidato -> TIPO_POR_DEFECTO.equals(candidato.nombre())).findFirst()
                        .orElseThrow(() -> new ReglaNegocioExcepcion("Indique el tipo de proveedor"))
                : buscar(tipos, comando.idTipoProveedor(), "el tipo de proveedor");
        return new DatosProveedor(tipo, comando.razonSocial(), comando.direccion(), documentos(comando, idProveedor),
                comando.telefono(), comando.email(), comando.servicios());
    }

    /** Un mismo documento (tipo, numero y complemento) no puede pertenecer a dos proveedores. */
    private List<DocumentoProveedor> documentos(ComandoProveedor comando, Integer idProveedor) {
        if (comando.documentos() == null) {
            return List.of();
        }
        List<ElementoCatalogo> tipos = repositorio.tiposDocumento();
        return comando.documentos().stream().map(documento -> {
            DocumentoProveedor nuevo = new DocumentoProveedor(documento.id(),
                    buscar(tipos, documento.idTipoDocumento(), "el tipo de documento"), documento.numero(),
                    documento.complemento());
            if (repositorio.existeDocumento(nuevo.tipo().id(), nuevo.numero(), nuevo.complemento(), idProveedor)) {
                throw new DatoDuplicadoExcepcion("El " + nuevo.tipo().nombre() + " " + nuevo.descripcion()
                        + " ya está registrado en otro proveedor");
            }
            return nuevo;
        }).toList();
    }

    private static ElementoCatalogo buscar(List<ElementoCatalogo> catalogo, Integer id, String entidad) {
        return catalogo.stream().filter(elemento -> elemento.id().equals(id)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(entidad, id));
    }
}
