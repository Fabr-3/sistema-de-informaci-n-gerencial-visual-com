package com.visual.backend.finanzas.infraestructura.inventario;

import org.springframework.stereotype.Component;

import com.visual.backend.activos.aplicacion.servicio.ServicioActivos;
import com.visual.backend.activos.aplicacion.servicio.ServicioLotes;
import com.visual.backend.activos.dominio.modelo.TipoMovimientoLote;
import com.visual.backend.finanzas.dominio.puerto.InventarioVentas;

/** Aplica las ventas sobre el inventario mediante los casos de uso del modulo de activos. */
@Component
public class InventarioVentasActivos implements InventarioVentas {

    private final ServicioActivos activos;
    private final ServicioLotes lotes;

    public InventarioVentasActivos(ServicioActivos activos, ServicioLotes lotes) {
        this.activos = activos;
        this.lotes = lotes;
    }

    @Override
    public void venderUnidad(Integer idActivo, String referencia) {
        activos.vender(idActivo, referencia);
    }

    @Override
    public void venderLote(Integer idExistencia, int cantidad, String referencia) {
        lotes.moverPorVenta(idExistencia, TipoMovimientoLote.VENTA, cantidad, referencia);
    }

    @Override
    public void revertirUnidad(Integer idActivo, String referencia) {
        activos.revertirVenta(idActivo, referencia);
    }

    @Override
    public void revertirLote(Integer idExistencia, int cantidad, String referencia) {
        lotes.moverPorVenta(idExistencia, TipoMovimientoLote.ANULACION_VENTA, cantidad, referencia);
    }
}
