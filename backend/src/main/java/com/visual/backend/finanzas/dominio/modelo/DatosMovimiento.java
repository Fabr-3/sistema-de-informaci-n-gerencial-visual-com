package com.visual.backend.finanzas.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos de un movimiento nuevo; proyecto, evento, proveedor y factura son opcionales. */
public record DatosMovimiento(TipoMovimiento tipo, String concepto, BigDecimal monto, LocalDate fecha,
                              Referencia proyecto, Referencia evento, Referencia proveedor, String numeroFactura,
                              List<ItemVenta> items) {
}
