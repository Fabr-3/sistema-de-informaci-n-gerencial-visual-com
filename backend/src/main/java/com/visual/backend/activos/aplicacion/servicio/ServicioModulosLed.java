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
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.EstadoModuloLed;
import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioModulosLed;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/** Modulos LED de las pantallas, rastreados uno por uno. */
@Service
public class ServicioModulosLed {

    public static final String ENTIDAD = "modulos_led";
    private static final String CAMPO_ESTADO = "estado";

    private final RepositorioModulosLed repositorio;
    private final RepositorioActivos activos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioModulosLed(RepositorioModulosLed repositorio, RepositorioActivos activos,
                              ServicioAuditoria auditoria, ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
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
        ModuloLed modulo = ModuloLed.nuevo(pantalla, codigo, LocalDateTime.now(reloj));
        if (repositorio.existeCodigo(modulo.getCodigo())) {
            throw new DatoDuplicadoExcepcion("El módulo " + modulo.getCodigo() + " ya está registrado");
        }
        ModuloLed guardado = repositorio.guardar(modulo, contexto.idUsuarioActual());
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null,
                Map.of("activo", pantalla.getCodigoInventario(), "codigo", guardado.getCodigo()), null);
        return guardado;
    }

    @Transactional
    public ModuloLed cambiarEstado(Integer idActivo, Integer idModulo, EstadoModuloLed estado) {
        activos.obtenerVigente(idActivo);
        ModuloLed modulo = repositorio.buscarPorId(idModulo)
                .filter(encontrado -> encontrado.getIdActivoPadre().equals(idActivo))
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el módulo", idModulo));
        EstadoModuloLed anterior = modulo.getEstado();
        modulo.cambiarEstado(estado, LocalDateTime.now(reloj));
        ModuloLed guardado = repositorio.guardar(modulo, contexto.idUsuarioActual());
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idModulo,
                Map.of(CAMPO_ESTADO, anterior.name()), Map.of(CAMPO_ESTADO, estado.name()), guardado.getCodigo());
        return guardado;
    }
}
