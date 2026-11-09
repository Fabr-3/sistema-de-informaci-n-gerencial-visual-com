package com.visual.backend.activos.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.visual.backend.activos.aplicacion.servicio.ParametrosActivos;

@Configuration
public class ConfiguracionActivos {

    @Bean
    public ParametrosActivos parametrosActivos(PropiedadesActivos propiedades) {
        return new ParametrosActivos(propiedades.porcentajeAlertaMantenimiento());
    }
}
