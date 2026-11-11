package com.visual.backend.gerencial.dominio.puerto;

import java.util.List;

import com.visual.backend.gerencial.dominio.modelo.FilaEvento;
import com.visual.backend.gerencial.dominio.modelo.FilaInventario;
import com.visual.backend.gerencial.dominio.modelo.FilaLote;
import com.visual.backend.gerencial.dominio.modelo.FilaMantenimiento;
import com.visual.backend.gerencial.dominio.modelo.Periodo;

/** Datos de los reportes operativos. */
public interface ConsultaReportes {

    /** Eventos que se realizan, total o parcialmente, en el periodo; por fecha de inicio. */
    List<FilaEvento> eventos(Periodo periodo);

    /** Mantenimientos en curso durante el periodo; por fecha de inicio. */
    List<FilaMantenimiento> mantenimientos(Periodo periodo);

    /** Situacion actual de los equipos por unidad, por categoria. */
    List<FilaInventario> inventario();

    /** Existencias actuales de los modelos por lote con saldo. */
    List<FilaLote> lotes();
}
