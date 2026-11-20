package com.visual.backend.subcontratacion.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.aplicacion.dto.ArchivoAdjunto;
import com.visual.backend.subcontratacion.aplicacion.dto.ComandoGasto;
import com.visual.backend.subcontratacion.aplicacion.dto.VistaGasto;
import com.visual.backend.subcontratacion.dominio.modelo.Comprobante;
import com.visual.backend.subcontratacion.dominio.modelo.DatosGasto;
import com.visual.backend.subcontratacion.dominio.modelo.EventoAsignado;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.GastoEvento;
import com.visual.backend.subcontratacion.dominio.puerto.AlmacenComprobantes;
import com.visual.backend.subcontratacion.dominio.puerto.AutorizacionConsumos;
import com.visual.backend.subcontratacion.dominio.puerto.RegistroEgresos;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioGastos;

/**
 * Gastos del evento con comprobante. Registran en la misma transaccion su
 * egreso financiero (imputado al proyecto y al evento) y anularlos lo anula.
 * El tecnico solo registra y ve gastos de los eventos donde esta asignado,
 * igual que los consumos.
 */
@Service
public class ServicioGastosEvento {

    public static final String ENTIDAD = "gastos_evento";
    static final String MENSAJE_NO_ASIGNADO = "Solo puede registrar gastos de los eventos donde está asignado";

    private final RepositorioGastos repositorio;
    private final AlmacenComprobantes almacen;
    private final ReferenciasImputacion referencias;
    private final ServicioConsumos consumos;
    private final AutorizacionConsumos autorizacion;
    private final RegistroEgresos egresos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioGastosEvento(RepositorioGastos repositorio, AlmacenComprobantes almacen,
                                ReferenciasImputacion referencias, ServicioConsumos consumos,
                                AutorizacionConsumos autorizacion, RegistroEgresos egresos,
                                ServicioAuditoria auditoria, ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.almacen = almacen;
        this.referencias = referencias;
        this.consumos = consumos;
        this.autorizacion = autorizacion;
        this.egresos = egresos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaGasto> listar(Filtros.Gastos filtro, SolicitudPagina solicitud) {
        Filtros.Gastos alcance = autorizacion.limitadoAEventosAsignados()
                ? filtro.soloEventos(consumos.eventosAsignados().stream().map(EventoAsignado::idEvento).toList())
                : filtro;
        return repositorio.buscar(alcance, solicitud).mapear(VistaGasto::de);
    }

    /**
     * Valida el comprobante, lo guarda y registra el gasto y su egreso. Si la
     * transaccion se revierte, el archivo guardado se elimina.
     */
    @Transactional
    public VistaGasto registrar(ComandoGasto comando, ArchivoAdjunto archivo) {
        exigirAlcance(comando.idEvento());
        ElementoCatalogo proyecto = referencias.proyectoVigente(comando.idProyecto());
        ElementoCatalogo evento = referencias.evento(comando.idEvento(), proyecto.id());
        Comprobante comprobante = Comprobante.recibido(archivo.nombre(), archivo.tipoContenido(),
                archivo.contenido());
        GastoEvento gasto = GastoEvento.registrar(new DatosGasto(proyecto, evento, comando.descripcion(),
                comando.monto(), comando.fecha(), comprobante), contexto.idUsuarioActual(), ahora());
        almacen.guardar(comprobante.archivo(), archivo.contenido());
        eliminarSiSeRevierte(comprobante.archivo());
        gasto.vincularEgreso(egresos.registrarGastoEvento(gasto.concepto(), gasto.getMonto(), gasto.getFecha(),
                proyecto.id(), evento.id()));
        GastoEvento guardado = repositorio.guardar(gasto);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null,
                VistaGasto.mapaAuditoria(guardado), null);
        return VistaGasto.de(guardado);
    }

    @Transactional(readOnly = true)
    public ArchivoAdjunto comprobante(Integer id) {
        GastoEvento gasto = repositorio.obtener(id);
        exigirAlcance(gasto.getEvento().id());
        Comprobante comprobante = gasto.getComprobante();
        byte[] contenido = almacen.leer(comprobante.archivo())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el comprobante del gasto", id));
        return new ArchivoAdjunto(comprobante.nombreOriginal(), comprobante.tipoContenido(), contenido);
    }

    @Transactional
    public VistaGasto anular(Integer id, String motivo) {
        GastoEvento gasto = repositorio.obtener(id);
        exigirAlcance(gasto.getEvento().id());
        Map<String, Object> anterior = VistaGasto.mapaAuditoria(gasto);
        gasto.anular(motivo, contexto.idUsuarioActual(), ahora());
        egresos.anular(gasto.getIdMovimiento(), gasto.getMotivoAnulacion());
        GastoEvento anulado = repositorio.guardar(gasto);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior,
                VistaGasto.mapaAuditoria(anulado), "Anulación del gasto del evento");
        return VistaGasto.de(anulado);
    }

    /** El tecnico solo opera sobre eventos donde esta asignado. */
    private void exigirAlcance(Integer idEvento) {
        if (autorizacion.limitadoAEventosAsignados() && consumos.eventosAsignados().stream()
                .noneMatch(evento -> evento.idEvento().equals(idEvento))) {
            throw new AccessDeniedException(MENSAJE_NO_ASIGNADO);
        }
    }

    private void eliminarSiSeRevierte(String archivo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int estado) {
                    if (estado != STATUS_COMMITTED) {
                        almacen.eliminar(archivo);
                    }
                }
            });
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
