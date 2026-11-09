package com.visual.backend.activos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.activos.aplicacion.dto.ComandoFinalizarMantenimiento;
import com.visual.backend.activos.aplicacion.dto.ComandoIniciarMantenimiento;
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioMantenimientos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/**
 * Mantenimientos de activos. Iniciar uno pone el activo en mantenimiento;
 * al finalizarlo vuelve a estar disponible o se da de baja si no es reparable.
 */
@Service
public class ServicioMantenimientos {

    public static final String ENTIDAD = "mantenimientos";

    private final RepositorioMantenimientos repositorio;
    private final RepositorioActivos activos;
    private final RepositorioCatalogosActivos catalogos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioMantenimientos(RepositorioMantenimientos repositorio, RepositorioActivos activos,
                                  RepositorioCatalogosActivos catalogos, ServicioAuditoria auditoria,
                                  ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.activos = activos;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<Mantenimiento> listar(Integer idActivo, SolicitudPagina solicitud) {
        activos.obtenerVigente(idActivo);
        return repositorio.listarPorActivo(idActivo, solicitud);
    }

    @Transactional
    public Mantenimiento iniciar(Integer idActivo, ComandoIniciarMantenimiento comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Integer actor = contexto.idUsuarioActual();
        Activo activo = activos.obtenerVigente(idActivo);
        TipoMantenimiento tipo = catalogos.buscarTipoMantenimiento(comando.idTipoMantenimiento())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de mantenimiento",
                        comando.idTipoMantenimiento()));

        activo.iniciarMantenimiento(actor, ahora);
        Mantenimiento mantenimiento = repositorio.guardar(Mantenimiento.iniciar(idActivo, tipo,
                comando.fechaInicio(), comando.descripcion(), actor, ahora));
        activos.guardar(activo);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, mantenimiento.getId(), null,
                mapa(activo, mantenimiento), "Inicio de mantenimiento");
        return mantenimiento;
    }

    @Transactional
    public Mantenimiento finalizar(Integer idActivo, Integer idMantenimiento, ComandoFinalizarMantenimiento comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Integer actor = contexto.idUsuarioActual();
        Activo activo = activos.obtenerVigente(idActivo);
        Mantenimiento mantenimiento = repositorio.buscarPorId(idMantenimiento)
                .filter(encontrado -> encontrado.getIdActivo().equals(idActivo))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el mantenimiento", idMantenimiento));
        Map<String, Object> anterior = mapa(activo, mantenimiento);

        mantenimiento.finalizar(comando.fechaFin(), comando.condicionFinal(), comando.observaciones(),
                comando.costo(), actor, ahora);
        activo.finalizarMantenimiento(comando.condicionFinal(), comando.observaciones(), actor, ahora);
        Mantenimiento guardado = repositorio.guardar(mantenimiento);
        activos.guardar(activo);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idMantenimiento, anterior,
                mapa(activo, guardado), "Finalización de mantenimiento");
        return guardado;
    }

    private static Map<String, Object> mapa(Activo activo, Mantenimiento mantenimiento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("activo", activo.getCodigoInventario());
        mapa.put("estadoActivo", activo.getEstado().name());
        mapa.put("tipo", mantenimiento.getTipo().nombre());
        mapa.put("estado", mantenimiento.getEstado().name());
        mapa.put("fechaInicio", mantenimiento.getFechaInicio().toString());
        mapa.put("fechaFin", mantenimiento.getFechaFin() == null ? null : mantenimiento.getFechaFin().toString());
        mapa.put("condicionFinal", mantenimiento.getCondicionFinal() == null ? null
                : mantenimiento.getCondicionFinal().name());
        mapa.put("costo", mantenimiento.getCosto());
        return mapa;
    }
}
