package com.visual.backend.gerencial.dominio.modelo;

import java.time.LocalDateTime;

/** Evento del reporte operativo, con el personal asignado y sus jornadas de montaje y desmontaje. */
public record FilaEvento(LocalDateTime inicio, LocalDateTime fin, String evento, String proyecto, String cliente,
                         String lugar, String estado, int personal, int jornadas) {
}
