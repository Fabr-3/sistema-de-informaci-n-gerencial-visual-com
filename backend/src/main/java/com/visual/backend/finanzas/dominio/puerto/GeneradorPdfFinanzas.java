package com.visual.backend.finanzas.dominio.puerto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;

/** Documentos PDF del modulo financiero. */
public interface GeneradorPdfFinanzas {

    /** Resultado del proyecto con sus movimientos registrados, el resumen por tipo y por evento. */
    byte[] movimientosProyecto(DetalleResultadoProyecto detalle, List<MovimientoFinanciero> movimientos,
                               LocalDateTime generado);
}
