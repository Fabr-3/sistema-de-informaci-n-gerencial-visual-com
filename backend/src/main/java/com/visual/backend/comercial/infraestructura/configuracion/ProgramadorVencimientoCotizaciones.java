package com.visual.backend.comercial.infraestructura.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.visual.backend.comercial.aplicacion.servicio.ServicioCotizaciones;

/** Marca como vencidas, una vez al dia, las cotizaciones en borrador cuya validez paso. */
@Component
public class ProgramadorVencimientoCotizaciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ProgramadorVencimientoCotizaciones.class);

    private final ServicioCotizaciones servicio;

    public ProgramadorVencimientoCotizaciones(ServicioCotizaciones servicio) {
        this.servicio = servicio;
    }

    @Scheduled(cron = "${app.comercial.cron-vencimiento-cotizaciones}")
    public void vencerCotizaciones() {
        int vencidas = servicio.vencerCaducadas();
        if (vencidas > 0) {
            REGISTRO.info("Cotizaciones vencidas automaticamente: {}", vencidas);
        }
    }
}
