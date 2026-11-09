package com.visual.backend.respaldos.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.visual.backend.respaldos.aplicacion.ServicioRespaldos;
import com.visual.backend.respaldos.dominio.Respaldo;
import com.visual.backend.respaldos.dominio.RespaldoExcepcion;

/** Respaldo automatico periodico (se desactiva con app.respaldos.programado=false). */
@Configuration
@ConditionalOnProperty(prefix = "app.respaldos", name = "programado", havingValue = "true")
public class ProgramadorRespaldos {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ProgramadorRespaldos.class);

    private final ServicioRespaldos servicio;

    public ProgramadorRespaldos(ServicioRespaldos servicio) {
        this.servicio = servicio;
    }

    @Scheduled(cron = "${app.respaldos.cron}")
    public void generarRespaldoProgramado() {
        try {
            Respaldo respaldo = servicio.generar();
            REGISTRO.info("Respaldo programado generado: {}", respaldo.nombreArchivo());
        } catch (RespaldoExcepcion e) {
            REGISTRO.error("Fallo el respaldo programado", e);
        }
    }
}
