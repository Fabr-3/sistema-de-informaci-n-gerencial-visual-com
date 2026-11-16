package com.visual.backend.subcontratacion.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoSubcontratacion;
import com.visual.backend.subcontratacion.dominio.modelo.DatosSubcontratacion;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.Subcontratacion;
import com.visual.backend.subcontratacion.dominio.puerto.RegistroEgresos;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioSubcontrataciones;

/**
 * Servicios contratados a proveedores para los proyectos. Cada uno registra
 * en la misma transaccion su egreso financiero, y anularlo anula el egreso.
 */
@Service
public class ServicioSubcontrataciones {

    public static final String ENTIDAD = "subcontrataciones";

    private final RepositorioSubcontrataciones repositorio;
    private final ReferenciasImputacion referencias;
    private final RegistroEgresos egresos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioSubcontrataciones(RepositorioSubcontrataciones repositorio, ReferenciasImputacion referencias,
                                     RegistroEgresos egresos, ServicioAuditoria auditoria,
                                     ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.referencias = referencias;
        this.egresos = egresos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<Subcontratacion> listar(Filtros.Subcontrataciones filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud);
    }

    @Transactional(readOnly = true)
    public Subcontratacion obtener(Integer id) {
        return repositorio.obtener(id);
    }

    @Transactional
    public Subcontratacion registrar(ComandoSubcontratacion comando) {
        ElementoCatalogo proveedor = referencias.proveedorVigente(comando.idProveedor());
        ElementoCatalogo proyecto = referencias.proyectoVigente(comando.idProyecto());
        Subcontratacion subcontratacion = Subcontratacion.registrar(new DatosSubcontratacion(proveedor,
                referencias.servicio(comando.idServicio(), proveedor.id()), proyecto,
                referencias.evento(comando.idEvento(), proyecto.id()), comando.descripcion(), comando.costo(),
                comando.fecha()), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        subcontratacion.vincularEgreso(egresos.registrar(subcontratacion.concepto(), subcontratacion.getCosto(),
                subcontratacion.getFecha(), proyecto.id(), comando.idEvento(), proveedor.id()));
        Subcontratacion guardada = repositorio.guardar(subcontratacion);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardada.getId(), null, mapa(guardada), null);
        return guardada;
    }

    @Transactional
    public Subcontratacion anular(Integer id, String motivo) {
        Subcontratacion subcontratacion = repositorio.obtener(id);
        Map<String, Object> anterior = mapa(subcontratacion);
        subcontratacion.anular(motivo, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        egresos.anular(subcontratacion.getIdMovimiento(), subcontratacion.getMotivoAnulacion());
        Subcontratacion anulada = repositorio.guardar(subcontratacion);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior, mapa(anulada),
                "Anulación de la subcontratación");
        return anulada;
    }

    private static Map<String, Object> mapa(Subcontratacion subcontratacion) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("proveedor", subcontratacion.getProveedor().nombre());
        mapa.put("servicio", subcontratacion.getServicio() == null ? null : subcontratacion.getServicio().nombre());
        mapa.put("proyecto", subcontratacion.getProyecto().nombre());
        mapa.put("evento", subcontratacion.getEvento() == null ? null : subcontratacion.getEvento().nombre());
        mapa.put("descripcion", subcontratacion.getDescripcion());
        mapa.put("costo", subcontratacion.getCosto().toPlainString());
        mapa.put("fecha", subcontratacion.getFecha().toString());
        mapa.put("estado", subcontratacion.getEstado().name());
        mapa.put("motivoAnulacion", subcontratacion.getMotivoAnulacion());
        mapa.put("egreso", subcontratacion.getIdMovimiento());
        return mapa;
    }
}
