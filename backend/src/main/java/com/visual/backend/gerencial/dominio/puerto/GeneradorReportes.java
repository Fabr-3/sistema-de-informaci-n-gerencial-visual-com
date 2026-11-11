package com.visual.backend.gerencial.dominio.puerto;

import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.FilaEvento;
import com.visual.backend.gerencial.dominio.modelo.FilaInventario;
import com.visual.backend.gerencial.dominio.modelo.FilaLote;
import com.visual.backend.gerencial.dominio.modelo.FilaMantenimiento;
import com.visual.backend.gerencial.dominio.modelo.KpisGerenciales;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionRecursos;

/** Documentos de los reportes operativos; generado es el momento de emision que se imprime. */
public interface GeneradorReportes {

    byte[] resumenGerencial(KpisGerenciales kpis, List<AlertaActivo> alertas, LocalDateTime generado);

    byte[] eventos(Periodo periodo, List<FilaEvento> eventos, LocalDateTime generado);

    byte[] estadoActivos(List<FilaInventario> inventario, List<FilaLote> lotes, List<AlertaActivo> alertas,
                         LocalDateTime generado);

    byte[] mantenimientos(Periodo periodo, List<FilaMantenimiento> mantenimientos, LocalDateTime generado);

    byte[] personal(Periodo periodo, UtilizacionRecursos utilizacion, LocalDateTime generado);
}
