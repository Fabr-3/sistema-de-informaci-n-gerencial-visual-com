package com.visual.backend.comercial.dominio.puerto;

import com.visual.backend.comercial.dominio.modelo.ResumenComercialCliente;

/** Lectura agregada de las cotizaciones y proyectos de un cliente. */
public interface ConsultaResumenCliente {

    /** limiteRecientes: cantidad de cotizaciones y de proyectos recientes que se devuelven. */
    ResumenComercialCliente resumen(Integer idCliente, int limiteRecientes);
}
