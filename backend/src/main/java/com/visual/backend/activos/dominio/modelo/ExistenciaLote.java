package com.visual.backend.activos.dominio.modelo;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Cantidad disponible de un modelo controlado por lote en una bodega. */
public class ExistenciaLote {

    private final Integer id;
    private final ModeloActivo modelo;
    private final Bodega bodega;
    private int cantidad;

    public ExistenciaLote(Integer id, ModeloActivo modelo, Bodega bodega, int cantidad) {
        this.id = id;
        this.modelo = modelo;
        this.bodega = bodega;
        this.cantidad = cantidad;
    }

    public static ExistenciaLote nueva(ModeloActivo modelo, Bodega bodega) {
        if (modelo.controlaPorUnidad()) {
            throw new ReglaNegocioExcepcion("El modelo " + modelo.nombreCompleto()
                    + " se controla por unidad: registre cada equipo como activo");
        }
        if (!bodega.activa()) {
            throw new ReglaNegocioExcepcion("La bodega " + bodega.nombre() + " está inactiva");
        }
        return new ExistenciaLote(null, modelo, bodega, 0);
    }

    /**
     * Aplica un movimiento y devuelve la cantidad con signo registrada. La
     * existencia nunca puede quedar negativa.
     */
    public int aplicar(TipoMovimientoLote tipo, int cantidadSolicitada) {
        if (cantidadSolicitada == 0) {
            throw new ReglaNegocioExcepcion("La cantidad del movimiento no puede ser cero");
        }
        if (tipo != TipoMovimientoLote.AJUSTE && cantidadSolicitada < 0) {
            throw new ReglaNegocioExcepcion("La cantidad debe ser positiva; solo los ajustes admiten negativos");
        }
        int variacion = tipo.cantidadConSigno(cantidadSolicitada);
        if (variacion > 0 && !bodega.activa()) {
            throw new ReglaNegocioExcepcion("La bodega " + bodega.nombre() + " está inactiva: solo admite salidas");
        }
        if (cantidad + variacion < 0) {
            throw new ReglaNegocioExcepcion("Existencia insuficiente en " + bodega.nombre() + ": hay " + cantidad
                    + " unidades de " + modelo.nombreCompleto());
        }
        cantidad += variacion;
        return variacion;
    }

    public Integer getId() {
        return id;
    }

    public ModeloActivo getModelo() {
        return modelo;
    }

    public Bodega getBodega() {
        return bodega;
    }

    public int getCantidad() {
        return cantidad;
    }
}
