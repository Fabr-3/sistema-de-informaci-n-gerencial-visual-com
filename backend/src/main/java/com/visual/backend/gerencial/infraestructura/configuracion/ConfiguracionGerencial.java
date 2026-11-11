package com.visual.backend.gerencial.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.visual.backend.gerencial.aplicacion.servicio.ParametrosGerencial;

@Configuration
public class ConfiguracionGerencial {

    @Bean
    public ParametrosGerencial parametrosGerencial(PropiedadesGerencial propiedades) {
        return new ParametrosGerencial(propiedades.diasMantenimientoProlongado(), propiedades.diasSinUso(),
                propiedades.correctivosFallasRecurrentes(), propiedades.proyectosClienteRecurrente());
    }
}
