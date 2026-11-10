package com.visual.backend.activos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.activos.aplicacion.dto.ComandoMovimientoLote;
import com.visual.backend.activos.aplicacion.dto.ComandoTrasladoLote;
import com.visual.backend.activos.aplicacion.dto.VistaExistencia;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.ExistenciaLote;
import com.visual.backend.activos.dominio.modelo.FiltroExistencias;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.MovimientoLote;
import com.visual.backend.activos.dominio.modelo.TipoMovimientoLote;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioLotes;
import com.visual.backend.activos.dominio.puerto.RepositorioModelos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Inventario de activos controlados por lote. Cada cambio de cantidad queda
 * como movimiento; la existencia se bloquea durante la operacion.
 */
@Service
public class ServicioLotes {

    public static final String ENTIDAD = "movimientos_lote";

    private final RepositorioLotes repositorio;
    private final RepositorioModelos modelos;
    private final RepositorioCatalogosActivos catalogos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioLotes(RepositorioLotes repositorio, RepositorioModelos modelos,
                         RepositorioCatalogosActivos catalogos, ServicioAuditoria auditoria,
                         ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.modelos = modelos;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<VistaExistencia> listar(FiltroExistencias filtro) {
        return repositorio.buscar(filtro).stream().map(VistaExistencia::de).toList();
    }

    @Transactional(readOnly = true)
    public Pagina<MovimientoLote> listarMovimientos(Integer idExistencia, SolicitudPagina solicitud) {
        repositorio.buscarPorId(idExistencia)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la existencia", idExistencia));
        return repositorio.listarMovimientos(idExistencia, solicitud);
    }

    @Transactional
    public VistaExistencia registrarMovimiento(ComandoMovimientoLote comando) {
        if (comando.tipo().esTraslado()) {
            throw new ReglaNegocioExcepcion("Los traslados se registran con la operación de traslado");
        }
        if (comando.tipo().esDeVenta()) {
            throw new ReglaNegocioExcepcion("Las ventas se registran como movimiento financiero");
        }
        ExistenciaLote existencia = mover(comando.idModelo(), comando.idBodega(), comando.tipo(),
                comando.cantidad(), comando.motivo());
        return VistaExistencia.de(existencia);
    }

    /**
     * Salida (VENTA) o reingreso (ANULACION_VENTA) de existencias por una venta;
     * lo invoca el modulo financiero dentro de su transaccion.
     */
    @Transactional
    public VistaExistencia moverPorVenta(Integer idExistencia, TipoMovimientoLote tipo, int cantidad,
                                         String referencia) {
        if (!tipo.esDeVenta()) {
            throw new IllegalArgumentException("Tipo de movimiento ajeno a una venta: " + tipo);
        }
        ExistenciaLote existencia = repositorio.buscarPorId(idExistencia)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la existencia", idExistencia));
        return VistaExistencia.de(mover(existencia.getModelo().id(), existencia.getBodega().id(), tipo, cantidad,
                referencia));
    }

    /** Traslado entre bodegas: salida en el origen y entrada en el destino, en una sola transaccion. */
    @Transactional
    public List<VistaExistencia> trasladar(ComandoTrasladoLote comando) {
        if (comando.idBodegaOrigen().equals(comando.idBodegaDestino())) {
            throw new ReglaNegocioExcepcion("La bodega de destino debe ser distinta de la de origen");
        }
        ExistenciaLote origen = mover(comando.idModelo(), comando.idBodegaOrigen(),
                TipoMovimientoLote.TRASLADO_SALIDA, comando.cantidad(), comando.motivo());
        ExistenciaLote destino = mover(comando.idModelo(), comando.idBodegaDestino(),
                TipoMovimientoLote.TRASLADO_ENTRADA, comando.cantidad(), comando.motivo());
        return List.of(VistaExistencia.de(origen), VistaExistencia.de(destino));
    }

    private ExistenciaLote mover(Integer idModelo, Integer idBodega, TipoMovimientoLote tipo, int cantidad,
                                 String motivo) {
        ModeloActivo modelo = modelos.buscarPorId(idModelo)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el modelo", idModelo));
        Bodega bodega = catalogos.buscarBodega(idBodega)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la bodega", idBodega));
        ExistenciaLote existencia = repositorio.buscarParaActualizar(idModelo, idBodega)
                .orElseGet(() -> repositorio.guardar(ExistenciaLote.nueva(modelo, bodega)));
        int cantidadAnterior = existencia.getCantidad();

        int variacion = existencia.aplicar(tipo, cantidad);
        ExistenciaLote guardada = repositorio.guardar(existencia);
        String textoMotivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
        MovimientoLote movimiento = repositorio.registrarMovimiento(new MovimientoLote(null, guardada.getId(), tipo,
                variacion, textoMotivo, LocalDateTime.now(reloj), contexto.idUsuarioActual(), null));

        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, movimiento.id(),
                Map.of("cantidad", cantidadAnterior), mapa(guardada, tipo, variacion, textoMotivo), null);
        return guardada;
    }

    private static Map<String, Object> mapa(ExistenciaLote existencia, TipoMovimientoLote tipo, int variacion,
                                            String motivo) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("modelo", existencia.getModelo().nombreCompleto());
        mapa.put("bodega", existencia.getBodega().nombre());
        mapa.put("tipo", tipo.name());
        mapa.put("variacion", variacion);
        mapa.put("cantidad", existencia.getCantidad());
        mapa.put("motivo", motivo);
        return mapa;
    }
}
