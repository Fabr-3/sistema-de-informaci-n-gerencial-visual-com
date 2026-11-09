package com.visual.backend.comercial.aplicacion.dto;

import java.util.List;

import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.Evento;

/** Proyecto con la cotizacion que lo origino, sus eventos y sus contratos. */
public record VistaDetalleProyecto(VistaProyecto proyecto, VistaCotizacion cotizacion, List<Evento> eventos,
                                   List<Contrato> contratos) {
}
