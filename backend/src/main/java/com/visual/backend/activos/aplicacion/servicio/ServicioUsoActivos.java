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
import com.visual.backend.activos.aplicacion.dto.ComandoRegistroUso;
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioRegistrosUso;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Historial de uso de los activos: base del calculo de horas para el mantenimiento. */
@Service
public class ServicioUsoActivos {

    public static final String ENTIDAD = "registros_uso";

    private final RepositorioRegistrosUso repositorio;
    private final RepositorioActivos activos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioUsoActivos(RepositorioRegistrosUso repositorio, RepositorioActivos activos,
                              ServicioAuditoria auditoria, ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.activos = activos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<RegistroUso> listar(Integer idActivo, SolicitudPagina solicitud) {
        activos.obtenerVigente(idActivo);
        return repositorio.listarPorActivo(idActivo, solicitud);
    }

    @Transactional
    public RegistroUso registrar(Integer idActivo, ComandoRegistroUso comando) {
        Activo activo = activos.obtenerVigente(idActivo);
        activo.validarPuedeRegistrarUso();
        RegistroUso registro = RegistroUso.nuevo(idActivo, comando.fechaInicio(), comando.fechaFin(),
                comando.descripcion(), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        if (repositorio.existeSuperposicion(idActivo, registro.fechaInicio(), registro.fechaFin())) {
            throw new ReglaNegocioExcepcion("El periodo se superpone con otro uso registrado del activo "
                    + activo.getCodigoInventario());
        }
        RegistroUso guardado = repositorio.guardar(registro);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.id(), null,
                mapa(activo, guardado), null);
        return guardado;
    }

    /** Anula un registro erroneo; deja de contar para las horas de uso. */
    @Transactional
    public void anular(Integer idActivo, Integer idRegistro) {
        Activo activo = activos.obtenerVigente(idActivo);
        RegistroUso registro = repositorio.buscarPorId(idRegistro)
                .filter(encontrado -> encontrado.idActivo().equals(idActivo))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el registro de uso", idRegistro));
        repositorio.anular(idRegistro, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, idRegistro, mapa(activo, registro), null,
                "Anulación de registro de uso");
    }

    private static Map<String, Object> mapa(Activo activo, RegistroUso registro) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("activo", activo.getCodigoInventario());
        mapa.put("fechaInicio", registro.fechaInicio().toString());
        mapa.put("fechaFin", registro.fechaFin().toString());
        mapa.put("horas", registro.horas());
        mapa.put("descripcion", registro.descripcion());
        return mapa;
    }
}
