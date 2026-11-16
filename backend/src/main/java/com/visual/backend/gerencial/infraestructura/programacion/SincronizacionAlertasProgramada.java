package com.visual.backend.gerencial.infraestructura.programacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.visual.backend.gerencial.aplicacion.servicio.ServicioAlertasActivos;

/** Mantiene las alertas al dia aunque nadie abra el tablero (app.gerencial.cron-sincronizacion-alertas). */
@Component
public class SincronizacionAlertasProgramada {

    private static final Logger REGISTRO = LoggerFactory.getLogger(SincronizacionAlertasProgramada.class);

    private final ServicioAlertasActivos alertas;

    public SincronizacionAlertasProgramada(ServicioAlertasActivos alertas) {
        this.alertas = alertas;
    }

    @Scheduled(cron = "${app.gerencial.cron-sincronizacion-alertas}")
    public void sincronizar() {
        try {
            alertas.sincronizar();
        } catch (RuntimeException e) {
            REGISTRO.error("Fallo la sincronizacion programada de alertas", e);
        }
    }
}
