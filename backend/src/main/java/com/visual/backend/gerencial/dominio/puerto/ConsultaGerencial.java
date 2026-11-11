package com.visual.backend.gerencial.dominio.puerto;

import java.util.List;

import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.ResultadoCliente;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionCategoria;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionPersonal;

/** Agregados para los KPIs gerenciales; solo cuentan los movimientos registrados (no anulados). */
public interface ConsultaGerencial {

    /** Clientes con movimientos imputados a sus proyectos en el periodo. */
    List<ResultadoCliente> resultadosPorCliente(Periodo periodo);

    /** Equipos por unidad que siguen en el inventario, agrupados por categoria. */
    List<UtilizacionCategoria> utilizacionEquipos(Periodo periodo);

    /** Personal tecnico activo, del mas al menos ocupado. */
    List<UtilizacionPersonal> utilizacionPersonal(Periodo periodo);
}
