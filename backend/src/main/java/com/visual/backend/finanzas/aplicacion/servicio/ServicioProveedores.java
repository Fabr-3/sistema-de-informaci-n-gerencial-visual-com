package com.visual.backend.finanzas.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.aplicacion.dto.ComandoProveedor;
import com.visual.backend.finanzas.aplicacion.dto.VistaProveedor;
import com.visual.backend.finanzas.dominio.modelo.DatosProveedor;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.dominio.puerto.RepositorioProveedores;

/** Registro basico de proveedores de compras, alquileres y servicios externos. */
@Service
public class ServicioProveedores {

    public static final String ENTIDAD = "proveedores";

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

    @Transactional
    public VistaProveedor crear(ComandoProveedor comando) {
        exigirNitUnico(comando.nit(), null);
        VistaProveedor creado = VistaProveedor.de(repositorio.guardar(Proveedor.nuevo(datos(comando),
                contexto.idUsuarioActual(), LocalDateTime.now(reloj))));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null, creado.comoMapaAuditoria(),
                null);
        return creado;
    }

    @Transactional
    public VistaProveedor actualizar(Integer id, ComandoProveedor comando) {
        Proveedor proveedor = repositorio.obtener(id);
        VistaProveedor anterior = VistaProveedor.de(proveedor);
        exigirNitUnico(comando.nit(), id);
        proveedor.actualizar(datos(comando), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaProveedor actualizado = VistaProveedor.de(repositorio.guardar(proveedor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
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

    private void exigirNitUnico(String nit, Integer idExcluido) {
        if (nit != null && !nit.isBlank() && repositorio.existeNit(nit.trim(), idExcluido)) {
            throw new DatoDuplicadoExcepcion("El NIT " + nit.trim() + " ya está registrado en otro proveedor");
        }
    }

    private static DatosProveedor datos(ComandoProveedor comando) {
        return new DatosProveedor(comando.razonSocial(), comando.nit(), comando.telefono(), comando.email(),
                comando.servicios());
    }
}
