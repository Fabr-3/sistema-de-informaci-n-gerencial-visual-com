package com.visual.backend.compartido.infraestructura.configuracion;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj unico de la aplicacion. Inyectarlo (en lugar de usar now() directo)
 * permite controlar el tiempo en las pruebas.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
