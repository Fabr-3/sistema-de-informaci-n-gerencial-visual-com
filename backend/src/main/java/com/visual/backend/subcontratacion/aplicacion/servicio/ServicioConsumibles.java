package com.visual.backend.subcontratacion.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoAjusteConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoIngresoConsumible;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaConsumible;
import com.visual.backend.subcontratacion.dominio.modelo.Consumible;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioConsumibles;

/** Catalogo de consumibles con su stock; cada ingreso o ajuste queda como movimiento. */
@Service
public class ServicioConsumibles {

    public static final String ENTIDAD = "consumibles";

    private final RepositorioConsumibles repositorio;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioConsumibles(RepositorioConsumibles repositorio, ServicioAuditoria auditoria,
                               ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaConsumible> listar(Filtros.Consumibles filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud).mapear(VistaConsumible::de);
    }

    @Transactional(readOnly = true)
    public VistaConsumible obtener(Integer id) {
        return VistaConsumible.de(repositorio.obtener(id));
    }

    @Transactional(readOnly = true)
    public Pagina<MovimientoConsumible> movimientos(Integer id, SolicitudPagina solicitud) {
        repositorio.obtener(id);
        return repositorio.movimientos(id, solicitud);
    }

    @Transactional
    public VistaConsumible registrar(ComandoConsumible comando) {
        Consumible consumible = Consumible.nuevo(comando.datos(), contexto.idUsuarioActual(), ahora());
        exigirNombreLibre(consumible.getNombre(), null);
        Consumible guardado = repositorio.guardar(consumible);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null,
                VistaConsumible.mapaAuditoria(guardado), null);
        return VistaConsumible.de(guardado);
    }

    @Transactional
    public VistaConsumible actualizar(Integer id, ComandoConsumible comando) {
        Consumible consumible = repositorio.obtenerParaActualizar(id);
        Map<String, Object> anterior = VistaConsumible.mapaAuditoria(consumible);
        consumible.actualizar(comando.datos(), contexto.idUsuarioActual(), ahora());
        exigirNombreLibre(consumible.getNombre(), id);
        Consumible guardado = repositorio.guardar(consumible);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior,
                VistaConsumible.mapaAuditoria(guardado), null);
        return VistaConsumible.de(guardado);
    }

    @Transactional
    public VistaConsumible ingresar(Integer id, ComandoIngresoConsumible comando) {
        Consumible consumible = repositorio.obtenerParaActualizar(id);
        Map<String, Object> anterior = VistaConsumible.mapaAuditoria(consumible);
        MovimientoConsumible movimiento = consumible.ingresar(comando.cantidad(), comando.costoUnitario(),
                comando.motivo(), contexto.idUsuarioActual(), ahora());
        return guardarMovimiento(consumible, movimiento, anterior, "Ingreso de stock");
    }

    @Transactional
    public VistaConsumible ajustar(Integer id, ComandoAjusteConsumible comando) {
        Consumible consumible = repositorio.obtenerParaActualizar(id);
        Map<String, Object> anterior = VistaConsumible.mapaAuditoria(consumible);
        MovimientoConsumible movimiento = consumible.ajustar(comando.cantidad(), comando.motivo(),
                contexto.idUsuarioActual(), ahora());
        return guardarMovimiento(consumible, movimiento, anterior, "Ajuste de stock: " + movimiento.motivo());
    }

    private VistaConsumible guardarMovimiento(Consumible consumible, MovimientoConsumible movimiento,
                                              Map<String, Object> anterior, String detalle) {
        Consumible guardado = repositorio.guardar(consumible);
        repositorio.registrarMovimiento(movimiento);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, guardado.getId(), anterior,
                VistaConsumible.mapaAuditoria(guardado), detalle);
        return VistaConsumible.de(guardado);
    }

    private void exigirNombreLibre(String nombre, Integer idExcluido) {
        if (repositorio.existeNombre(nombre, idExcluido)) {
            throw new DatoDuplicadoExcepcion("Ya existe el consumible " + nombre);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
