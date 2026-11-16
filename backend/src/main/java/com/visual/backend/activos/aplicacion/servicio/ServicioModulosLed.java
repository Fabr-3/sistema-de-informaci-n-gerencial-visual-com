package com.visual.backend.activos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.activos.aplicacion.dto.ComandoReasignacionModulo;
import com.visual.backend.activos.aplicacion.dto.ComandoReemplazoModulo;
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.EntradaHistorialModulo;
import com.visual.backend.activos.dominio.modelo.EstadoModuloLed;
import com.visual.backend.activos.dominio.modelo.HistorialModuloLed;
import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioHistorialModulos;
import com.visual.backend.activos.dominio.puerto.RepositorioModulosLed;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/**
 * Modulos LED de las pantallas, rastreados uno por uno. Cada alta, cambio de
 * estado, reemplazo o reasignacion queda en su historial.
 */
@Service
public class ServicioModulosLed {

    public static final String ENTIDAD = "modulos_led";
    private static final String CAMPO_ESTADO = "estado";
    private static final String CAMPO_PANTALLA = "pantalla";

    private final RepositorioModulosLed repositorio;
    private final RepositorioHistorialModulos historial;
    private final RepositorioActivos activos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioModulosLed(RepositorioModulosLed repositorio, RepositorioHistorialModulos historial,
                              RepositorioActivos activos, ServicioAuditoria auditoria, ContextoSolicitud contexto,
                              Clock reloj) {
        this.repositorio = repositorio;
        this.historial = historial;
        this.activos = activos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ModuloLed> listar(Integer idActivo) {
        activos.obtenerVigente(idActivo);
        return repositorio.listarPorActivo(idActivo);
    }

    @Transactional
    public ModuloLed agregar(Integer idActivo, String codigo) {
        Activo pantalla = activos.obtenerVigente(idActivo);
        ModuloLed guardado = registrarNuevo(ModuloLed.nuevo(pantalla, codigo, ahora()));
        Integer actor = contexto.idUsuarioActual();
        historial.registrar(HistorialModuloLed.alta(guardado, actor));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null,
                Map.of(CAMPO_PANTALLA, pantalla.getCodigoInventario(), "codigo", guardado.getCodigo()), null);
        return guardado;
    }

    @Transactional
    public ModuloLed cambiarEstado(Integer idActivo, Integer idModulo, EstadoModuloLed estado) {
        ModuloLed modulo = moduloDe(idActivo, idModulo);
        EstadoModuloLed anterior = modulo.getEstado();
        modulo.cambiarEstado(estado, ahora());
        Integer actor = contexto.idUsuarioActual();
        ModuloLed guardado = repositorio.guardar(modulo, actor);
        historial.registrar(HistorialModuloLed.cambioEstado(guardado, anterior, actor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idModulo,
                Map.of(CAMPO_ESTADO, anterior.name()), Map.of(CAMPO_ESTADO, estado.name()), guardado.getCodigo());
        return guardado;
    }

    /** El modulo actual queda retirado y uno nuevo, activo, ocupa su lugar en la misma pantalla. */
    @Transactional
    public ModuloLed reemplazar(Integer idActivo, Integer idModulo, ComandoReemplazoModulo comando) {
        ModuloLed actual = moduloDe(idActivo, idModulo);
        EstadoModuloLed anterior = actual.getEstado();
        LocalDateTime ahora = ahora();
        ModuloLed nuevo = registrarNuevo(actual.reemplazar(comando.codigoNuevo(), ahora));
        Integer actor = contexto.idUsuarioActual();
        ModuloLed retirado = repositorio.guardar(actual, actor);
        String motivo = comando.motivo().trim();
        historial.registrar(HistorialModuloLed.reemplazo(retirado, anterior, nuevo, motivo, ahora, actor));
        historial.registrar(HistorialModuloLed.reemplazo(nuevo, null, retirado, motivo, ahora, actor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idModulo,
                Map.of(CAMPO_ESTADO, anterior.name()), Map.of(CAMPO_ESTADO, retirado.getEstado().name(),
                        "reemplazadoPor", nuevo.getCodigo()), motivo);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, nuevo.getId(), null,
                Map.of("codigo", nuevo.getCodigo(), "reemplazaA", retirado.getCodigo()), motivo);
        return nuevo;
    }

    /** Pasa un modulo activo a otra pantalla vigente. */
    @Transactional
    public ModuloLed reasignar(Integer idActivo, Integer idModulo, ComandoReasignacionModulo comando) {
        ModuloLed modulo = moduloDe(idActivo, idModulo);
        Activo origen = activos.obtenerVigente(idActivo);
        Activo destino = activos.obtenerVigente(comando.idActivoDestino());
        modulo.reasignar(destino, ahora());
        Integer actor = contexto.idUsuarioActual();
        ModuloLed guardado = repositorio.guardar(modulo, actor);
        String motivo = comando.motivo().trim();
        historial.registrar(HistorialModuloLed.reasignacion(guardado, idActivo, motivo, actor));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idModulo,
                Map.of(CAMPO_PANTALLA, origen.getCodigoInventario()),
                Map.of(CAMPO_PANTALLA, destino.getCodigoInventario()), motivo);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<EntradaHistorialModulo> historial(Integer idActivo, Integer idModulo) {
        return historial.historial(moduloDe(idActivo, idModulo).getId());
    }

    private ModuloLed registrarNuevo(ModuloLed modulo) {
        if (repositorio.existeCodigo(modulo.getCodigo())) {
            throw new DatoDuplicadoExcepcion("El módulo " + modulo.getCodigo() + " ya está registrado");
        }
        return repositorio.guardar(modulo, contexto.idUsuarioActual());
    }

    private ModuloLed moduloDe(Integer idActivo, Integer idModulo) {
        activos.obtenerVigente(idActivo);
        return repositorio.buscarPorId(idModulo)
                .filter(encontrado -> encontrado.getIdActivoPadre().equals(idActivo))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el módulo", idModulo));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
