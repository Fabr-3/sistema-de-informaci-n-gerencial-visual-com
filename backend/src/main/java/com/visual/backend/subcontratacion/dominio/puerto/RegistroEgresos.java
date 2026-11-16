package com.visual.backend.subcontratacion.dominio.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Egresos del modulo financiero que generan las subcontrataciones. */
public interface RegistroEgresos {

    /** Registra el egreso imputado al proyecto (y evento) con el proveedor; devuelve su id. */
    Integer registrar(String concepto, BigDecimal costo, LocalDate fecha, Integer idProyecto, Integer idEvento,
                      Integer idProveedor);

    void anular(Integer idMovimiento, String motivo);
}
