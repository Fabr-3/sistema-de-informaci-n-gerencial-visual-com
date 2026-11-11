package com.visual.backend.finanzas.aplicacion.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.aplicacion.dto.ComandoMovimiento;
import com.visual.backend.finanzas.aplicacion.dto.ListadoMovimientos;
import com.visual.backend.finanzas.dominio.modelo.DatosMovimiento;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.ItemVenta;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Referencia;
import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;
import com.visual.backend.finanzas.dominio.puerto.ConsultaComercialFinanzas;
import com.visual.backend.finanzas.dominio.puerto.InventarioVentas;
import com.visual.backend.finanzas.dominio.puerto.RepositorioMovimientos;
import com.visual.backend.finanzas.dominio.puerto.RepositorioProveedores;
import com.visual.backend.finanzas.dominio.puerto.RepositorioTiposMovimiento;

/**
 * Registro y anulacion de ingresos y egresos. Las ventas de equipos
 * descuentan el inventario en la misma transaccion y su anulacion lo repone.
 */
@Service
public class ServicioMovimientos {

    public static final String ENTIDAD = "movimientos_financieros";

    private final RepositorioMovimientos repositorio;
    private final RepositorioTiposMovimiento tipos;
    private final RepositorioProveedores proveedores;
    private final ConsultaComercialFinanzas comercial;
    private final InventarioVentas inventario;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioMovimientos(RepositorioMovimientos repositorio, RepositorioTiposMovimiento tipos,
                               RepositorioProveedores proveedores, ConsultaComercialFinanzas comercial,
                               InventarioVentas inventario, ServicioAuditoria auditoria, ContextoSolicitud contexto,
                               Clock reloj) {
        this.repositorio = repositorio;
        this.tipos = tipos;
        this.proveedores = proveedores;
        this.comercial = comercial;
        this.inventario = inventario;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<TipoMovimiento> tipos() {
        return tipos.listar();
    }

    @Transactional(readOnly = true)
    public ListadoMovimientos listar(Filtros.Movimientos filtro, SolicitudPagina solicitud) {
        return ListadoMovimientos.de(repositorio.buscar(filtro, solicitud), repositorio.totales(filtro));
    }

    @Transactional(readOnly = true)
    public MovimientoFinanciero obtener(Integer id) {
        return repositorio.obtener(id);
    }

    @Transactional
    public MovimientoFinanciero registrar(ComandoMovimiento comando) {
        Referencia proyecto = proyecto(comando.idProyecto());
        MovimientoFinanciero movimiento = MovimientoFinanciero.registrar(new DatosMovimiento(
                tipos.obtener(comando.idTipo()), comando.concepto(), comando.monto(), comando.fecha(), proyecto,
                evento(comando.idEvento(), proyecto), proveedor(comando.idProveedor()), comando.numeroFactura(),
                items(comando)), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        MovimientoFinanciero guardado = repositorio.guardar(movimiento);
        if (guardado.esVenta()) {
            guardado.getItems().forEach(item -> vender(item, guardado.referencia()));
        }
        MovimientoFinanciero resultado = repositorio.obtener(guardado.getId());
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, resultado.getId(), null, mapa(resultado), null);
        return resultado;
    }

    /** Egreso general automatico por el costo de un mantenimiento finalizado. */
    @Transactional
    public MovimientoFinanciero registrarCostoMantenimiento(Integer idMantenimiento, String concepto,
                                                            BigDecimal costo, LocalDate fecha) {
        TipoMovimiento tipo = tipos.buscarPorCodigoSistema(TipoMovimiento.CODIGO_MANTENIMIENTO)
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "No está configurado el tipo de movimiento para los costos de mantenimiento"));
        MovimientoFinanciero guardado = repositorio.guardar(MovimientoFinanciero.costoMantenimiento(tipo, concepto,
                costo, fecha, idMantenimiento, contexto.idUsuarioActual(), LocalDateTime.now(reloj)));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, guardado.getId(), null, mapa(guardado),
                "Costo de mantenimiento");
        return guardado;
    }

    /** La anulacion de una venta devuelve al inventario lo vendido. */
    @Transactional
    public MovimientoFinanciero anular(Integer id, String motivo) {
        MovimientoFinanciero movimiento = repositorio.obtener(id);
        Map<String, Object> anterior = mapa(movimiento);
        movimiento.anular(motivo, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        MovimientoFinanciero anulado = repositorio.guardar(movimiento);
        if (anulado.esVenta()) {
            anulado.getItems().forEach(item -> revertir(item, anulado.referencia()));
        }
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior, mapa(anulado),
                "Anulación del movimiento");
        return anulado;
    }

    private void vender(ItemVenta item, String referencia) {
        if (item.esUnidad()) {
            inventario.venderUnidad(item.idActivo(), referencia);
        } else {
            inventario.venderLote(item.idExistencia(), item.cantidad(), referencia);
        }
    }

    private void revertir(ItemVenta item, String referencia) {
        if (item.esUnidad()) {
            inventario.revertirUnidad(item.idActivo(), referencia);
        } else {
            inventario.revertirLote(item.idExistencia(), item.cantidad(), referencia);
        }
    }

    private Referencia proyecto(Integer idProyecto) {
        return idProyecto == null ? null : comercial.buscarProyecto(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proyecto", idProyecto));
    }

    private Referencia evento(Integer idEvento, Referencia proyecto) {
        if (idEvento == null || proyecto == null) {
            return idEvento == null ? null : new Referencia(idEvento, null);
        }
        return comercial.buscarEventoDelProyecto(idEvento, proyecto.id())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el evento del proyecto", idEvento));
    }

    private Referencia proveedor(Integer idProveedor) {
        return idProveedor == null ? null : proveedores.obtener(idProveedor).comoReferencia();
    }

    private static List<ItemVenta> items(ComandoMovimiento comando) {
        if (comando.items() == null) {
            return List.of();
        }
        return comando.items().stream()
                .map(item -> item.idActivo() != null ? ItemVenta.unidad(item.idActivo(), null)
                        : ItemVenta.lote(item.idExistencia(), item.cantidad() == null ? 0 : item.cantidad(), null))
                .toList();
    }

    private static Map<String, Object> mapa(MovimientoFinanciero movimiento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("tipo", movimiento.getTipo().nombre());
        mapa.put("concepto", movimiento.getConcepto());
        mapa.put("monto", movimiento.getMonto().toPlainString());
        mapa.put("fecha", movimiento.getFecha().toString());
        mapa.put("proyecto", movimiento.getProyecto() == null ? null : movimiento.getProyecto().nombre());
        mapa.put("evento", movimiento.getEvento() == null ? null : movimiento.getEvento().nombre());
        mapa.put("proveedor", movimiento.getProveedor() == null ? null : movimiento.getProveedor().nombre());
        mapa.put("numeroFactura", movimiento.getNumeroFactura());
        mapa.put("estado", movimiento.getEstado().name());
        mapa.put("motivoAnulacion", movimiento.getMotivoAnulacion());
        mapa.put("items", movimiento.getItems().stream().map(ItemVenta::descripcion).toList());
        return mapa;
    }
}
