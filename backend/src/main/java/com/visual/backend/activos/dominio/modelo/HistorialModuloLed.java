package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Cambio registrado de un modulo LED. idModuloRelacionado es el modulo que lo
 * reemplaza o al que reemplaza; los activos son la pantalla de origen y la de
 * destino (en el alta, solo el destino).
 */
public record HistorialModuloLed(Integer idModulo, TipoCambioModulo tipo, Integer idActivoOrigen,
                                 Integer idActivoDestino, EstadoModuloLed estadoAnterior,
                                 EstadoModuloLed estadoNuevo, Integer idModuloRelacionado, String motivo,
                                 LocalDateTime fecha, Integer registradoPor) {

    public static HistorialModuloLed alta(ModuloLed modulo, Integer actor) {
        return new HistorialModuloLed(modulo.getId(), TipoCambioModulo.ALTA, null, modulo.getIdActivoPadre(), null,
                modulo.getEstado(), null, null, modulo.getFechaCreacion(), actor);
    }

    public static HistorialModuloLed cambioEstado(ModuloLed modulo, EstadoModuloLed anterior, Integer actor) {
        return new HistorialModuloLed(modulo.getId(), TipoCambioModulo.CAMBIO_ESTADO, null, modulo.getIdActivoPadre(),
                anterior, modulo.getEstado(), null, null, modulo.getFechaModificacion(), actor);
    }

    /** Se registra en ambos modulos, cada uno apuntando al otro. */
    public static HistorialModuloLed reemplazo(ModuloLed modulo, EstadoModuloLed anterior, ModuloLed otro,
                                               String motivo, LocalDateTime fecha, Integer actor) {
        return new HistorialModuloLed(modulo.getId(), TipoCambioModulo.REEMPLAZO, null, modulo.getIdActivoPadre(),
                anterior, modulo.getEstado(), otro.getId(), motivo, fecha, actor);
    }

    public static HistorialModuloLed reasignacion(ModuloLed modulo, Integer idOrigen, String motivo, Integer actor) {
        return new HistorialModuloLed(modulo.getId(), TipoCambioModulo.REASIGNACION, idOrigen,
                modulo.getIdActivoPadre(), modulo.getEstado(), modulo.getEstado(), null, motivo,
                modulo.getFechaModificacion(), actor);
    }
}
