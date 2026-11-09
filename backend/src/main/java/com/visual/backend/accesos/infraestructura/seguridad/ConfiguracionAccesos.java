package com.visual.backend.accesos.infraestructura.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.visual.backend.accesos.aplicacion.servicio.ParametrosSesion;
import com.visual.backend.accesos.dominio.modelo.PoliticaBloqueo;

/** Expone la configuracion externa como objetos de dominio y aplicacion. */
@Configuration
public class ConfiguracionAccesos {

    @Bean
    public PoliticaBloqueo politicaBloqueo(PropiedadesSeguridad propiedades) {
        return new PoliticaBloqueo(propiedades.bloqueo().intentosMaximos(), propiedades.bloqueo().duracion());
    }

    @Bean
    public ParametrosSesion parametrosSesion(PropiedadesSeguridad propiedades) {
        return new ParametrosSesion(propiedades.refresco().duracion());
    }
}
