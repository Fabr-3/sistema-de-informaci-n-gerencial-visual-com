package com.visual.backend.accesos.aplicacion.servicio;

import java.time.Duration;

/** Parametros configurables de las sesiones (los define la infraestructura). */
public record ParametrosSesion(Duration duracionRefresco) {
}
