package com.visual.backend.accesos.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Registro inmutable de auditoria. Los valores anteriores y nuevos son
 * instantaneas de la entidad sin datos sensibles (nunca contrasenas).
 */
public record RegistroAuditoria(
        Long id,
        LocalDateTime fecha,
        Integer idUsuario,
        String nombreUsuario,
        AccionAuditoria accion,
        String entidad,
        String idEntidad,
        Map<String, Object> valoresAnteriores,
        Map<String, Object> valoresNuevos,
        String detalle,
        String direccionIp,
        String agenteUsuario) {
}
