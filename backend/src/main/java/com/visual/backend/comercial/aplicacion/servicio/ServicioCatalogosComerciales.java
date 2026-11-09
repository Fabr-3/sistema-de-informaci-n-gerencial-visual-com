package com.visual.backend.comercial.aplicacion.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoLugar;
import com.visual.backend.comercial.aplicacion.dto.ComandoServicio;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Servicio;
import com.visual.backend.comercial.dominio.puerto.RepositorioCatalogosComerciales;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/** Catalogos del modulo comercial: tipos fijos, servicios ofrecidos y lugares de eventos. */
@Service
public class ServicioCatalogosComerciales {

    private static final String ENTIDAD_SERVICIOS = "servicios";
    private static final String ENTIDAD_LUGARES = "lugares";

    private final RepositorioCatalogosComerciales repositorio;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;

    public ServicioCatalogosComerciales(RepositorioCatalogosComerciales repositorio, ServicioAuditoria auditoria,
                                        ContextoSolicitud contexto) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public List<ElementoCatalogo> listarTiposCliente() {
        return repositorio.listarTiposCliente();
    }

    @Transactional(readOnly = true)
    public List<ElementoCatalogo> listarTiposDocumento() {
        return repositorio.listarTiposDocumento();
    }

    @Transactional(readOnly = true)
    public List<ElementoCatalogo> listarTiposContacto() {
        return repositorio.listarTiposContacto();
    }

    @Transactional(readOnly = true)
    public List<Servicio> listarServicios(boolean soloActivos) {
        return repositorio.listarServicios(soloActivos);
    }

    @Transactional
    public Servicio guardarServicio(Integer id, ComandoServicio comando) {
        Servicio anterior = id == null ? null : repositorio.buscarServicios(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el servicio", id));
        Servicio servicio = new Servicio(id, comando.nombre(), comando.descripcion(), comando.unidadMedida(),
                comando.precioReferencial(), comando.activo());
        if (repositorio.existeServicio(servicio.nombre(), id)) {
            throw new DatoDuplicadoExcepcion("El servicio " + servicio.nombre() + " ya existe");
        }
        Servicio guardado = repositorio.guardarServicio(servicio, contexto.idUsuarioActual());
        auditoria.registrarOperacion(accion(id), ENTIDAD_SERVICIOS, guardado.id(),
                anterior == null ? null : mapa(anterior), mapa(guardado), null);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Lugar> listarLugares() {
        return repositorio.listarLugares();
    }

    @Transactional
    public Lugar guardarLugar(Integer id, ComandoLugar comando) {
        Lugar anterior = id == null ? null : repositorio.buscarLugar(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el lugar", id));
        Lugar lugar = new Lugar(id, comando.nombre(), comando.direccion(), comando.ciudad());
        if (repositorio.existeLugar(lugar.nombre(), lugar.direccion(), id)) {
            throw new DatoDuplicadoExcepcion("El lugar " + lugar.nombre() + " ya está registrado con esa dirección");
        }
        Lugar guardado = repositorio.guardarLugar(lugar, contexto.idUsuarioActual());
        auditoria.registrarOperacion(accion(id), ENTIDAD_LUGARES, guardado.id(),
                anterior == null ? null : mapa(anterior), mapa(guardado), null);
        return guardado;
    }

    private static AccionAuditoria accion(Integer id) {
        return id == null ? AccionAuditoria.CREAR : AccionAuditoria.ACTUALIZAR;
    }

    private static Map<String, Object> mapa(Servicio servicio) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", servicio.nombre());
        mapa.put("unidadMedida", servicio.unidadMedida());
        mapa.put("precioReferencial", servicio.precioReferencial());
        mapa.put("activo", servicio.activo());
        return mapa;
    }

    private static Map<String, Object> mapa(Lugar lugar) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", lugar.nombre());
        mapa.put("direccion", lugar.direccion());
        mapa.put("ciudad", lugar.ciudad());
        return mapa;
    }
}
