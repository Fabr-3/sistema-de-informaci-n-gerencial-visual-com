package com.visual.backend.finanzas.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.visual.backend.finanzas.aplicacion.servicio.ParametrosFinanzas;

@Configuration
public class ConfiguracionFinanzas {

    @Bean
    public ParametrosFinanzas parametrosFinanzas(PropiedadesFinanzas propiedades) {
        return new ParametrosFinanzas(propiedades.umbralMovimientoSignificativo(),
                propiedades.diasMovimientosRecientes());
    }
}
