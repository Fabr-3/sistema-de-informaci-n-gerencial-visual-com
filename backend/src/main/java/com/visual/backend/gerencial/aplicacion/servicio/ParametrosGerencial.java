package com.visual.backend.gerencial.aplicacion.servicio;

/**
 * Parametros configurables del modulo (los define la infraestructura).
 * diasMantenimientoProlongado: dias en mantenimiento desde los que se alerta;
 * diasSinUso: dias sin uso de un equipo disponible desde los que se alerta;
 * correctivosFallasRecurrentes: correctivos en el ultimo año que indican fallas recurrentes;
 * proyectosClienteRecurrente: proyectos contratados desde los que un cliente es recurrente.
 */
public record ParametrosGerencial(int diasMantenimientoProlongado, int diasSinUso, int correctivosFallasRecurrentes,
                                  int proyectosClienteRecurrente) {
}
