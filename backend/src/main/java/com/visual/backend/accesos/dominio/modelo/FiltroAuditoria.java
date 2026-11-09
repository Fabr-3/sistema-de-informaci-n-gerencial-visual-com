package com.visual.backend.accesos.dominio.modelo;

import java.time.LocalDateTime;

/** Criterios opcionales de consulta de auditoria (nulo = sin filtro). */
public record FiltroAuditoria(LocalDateTime desde, LocalDateTime hasta, Integer idUsuario,
                              AccionAuditoria accion, String entidad) {
}
