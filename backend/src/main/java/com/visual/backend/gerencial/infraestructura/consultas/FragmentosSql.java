package com.visual.backend.gerencial.infraestructura.consultas;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.visual.backend.gerencial.dominio.modelo.Periodo;

/** Partes de SQL comunes a las consultas del modulo. */
final class FragmentosSql {

    /** Activo con su modelo (fabricante y nombre) y categoria; requiere el alias a de activos. */
    static final String ACTIVO_CON_MODELO = """
            join modelos_activo mo on mo.id_modelo = a.id_modelo
            join fabricantes f on f.id_fabricante = mo.id_fabricante
            join categorias_activo c on c.id_categoria = mo.id_categoria
            """;
    static final String COLUMNAS_ACTIVO = "a.id_activo, a.codigo_inventario, f.nombre || ' ' || mo.nombre as modelo, "
            + "c.nombre as categoria";
    /** Activo que sigue en el inventario (no eliminado, ni de baja ni vendido). */
    static final String ACTIVO_VIGENTE = "a.fecha_eliminacion is null and a.estado_operativo not in ('BAJA', 'VENDIDO')";

    private FragmentosSql() {
    }

    static MapSqlParameterSource periodo(Periodo periodo) {
        return new MapSqlParameterSource().addValue("desde", periodo.desde()).addValue("hasta", periodo.hasta());
    }
}
