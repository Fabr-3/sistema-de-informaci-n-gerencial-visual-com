package com.visual.backend.subcontratacion.infraestructura.integracion;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.visual.backend.finanzas.aplicacion.servicio.ServicioMovimientos;
import com.visual.backend.subcontratacion.dominio.puerto.RegistroEgresos;

/** Registra y anula los egresos de las subcontrataciones con los casos de uso del modulo financiero. */
@Component
public class RegistroEgresosFinanzas implements RegistroEgresos {

    private final ServicioMovimientos movimientos;

    public RegistroEgresosFinanzas(ServicioMovimientos movimientos) {
        this.movimientos = movimientos;
    }

    @Override
    public Integer registrar(String concepto, BigDecimal costo, LocalDate fecha, Integer idProyecto, Integer idEvento,
                             Integer idProveedor) {
        return movimientos.registrarSubcontratacion(concepto, costo, fecha, idProyecto, idEvento, idProveedor)
                .getId();
    }

    @Override
    public Integer registrarGastoEvento(String concepto, BigDecimal monto, LocalDate fecha, Integer idProyecto,
                                        Integer idEvento) {
        return movimientos.registrarGastoEvento(concepto, monto, fecha, idProyecto, idEvento).getId();
    }

    @Override
    public void anular(Integer idMovimiento, String motivo) {
        movimientos.anularSubcontratacion(idMovimiento, motivo);
    }
}
