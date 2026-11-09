package com.visual.backend.comercial.dominio.puerto;

import java.util.List;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.Proyecto;

/** Genera los documentos PDF que se entregan al cliente. */
public interface GeneradorDocumentos {

    byte[] cotizacion(Cotizacion cotizacion, Cliente cliente);

    /** Contrato con la plantilla estandar, completado con los datos del proyecto. */
    byte[] contrato(Contrato contrato, Proyecto proyecto, Cotizacion cotizacion, Cliente cliente,
                    List<Evento> eventos);
}
