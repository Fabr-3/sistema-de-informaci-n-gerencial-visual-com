package com.visual.backend.finanzas.infraestructura.consultas;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.finanzas.dominio.puerto.ConsultaOrigenMovimiento;

/** Consulta de solo lectura sobre las tablas de otros modulos (mismo enfoque que gerencial). */
@Repository
public class ConsultaOrigenMovimientoJdbc implements ConsultaOrigenMovimiento {

    private static final String DE_SUBCONTRATACION =
            "select exists (select 1 from subcontrataciones where id_movimiento = :idMovimiento)"
                    + " or exists (select 1 from gastos_evento where id_movimiento = :idMovimiento)";

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaOrigenMovimientoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean esDeSubcontratacion(Integer idMovimiento) {
        return Boolean.TRUE.equals(jdbc.queryForObject(DE_SUBCONTRATACION,
                new MapSqlParameterSource("idMovimiento", idMovimiento), Boolean.class));
    }
}
