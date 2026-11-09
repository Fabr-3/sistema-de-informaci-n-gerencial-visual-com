package com.visual.backend.compartido.infraestructura.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas de todos los modulos (respaldos, vencimientos). */
@Configuration
@EnableScheduling
public class ConfiguracionProgramacion {
}
