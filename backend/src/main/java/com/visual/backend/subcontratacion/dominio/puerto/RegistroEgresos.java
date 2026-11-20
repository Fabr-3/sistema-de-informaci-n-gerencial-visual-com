package com.visual.backend.subcontratacion.dominio.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Egresos del modulo financiero que generan las subcontrataciones y los gastos del evento. */
public interface RegistroEgresos {

    /** Registra el egreso imputado al proyecto (y evento) con el proveedor; devuelve su id. */
    Integer registrar(String concepto, BigDecimal costo, LocalDate fecha, Integer idProyecto, Integer idEvento,
                      Integer idProveedor);

    /** Registra el egreso de un gasto del evento (sin proveedor); devuelve su id. */
    Integer registrarGastoEvento(String concepto, BigDecimal monto, LocalDate fecha, Integer idProyecto,
                                 Integer idEvento);

    void anular(Integer idMovimiento, String motivo);
}
