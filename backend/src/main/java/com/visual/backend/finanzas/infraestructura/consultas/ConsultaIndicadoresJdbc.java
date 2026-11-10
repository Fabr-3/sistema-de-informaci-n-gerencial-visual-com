package com.visual.backend.finanzas.infraestructura.consultas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.finanzas.dominio.modelo.MovimientoDestacado;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.SumasPeriodo;
import com.visual.backend.finanzas.dominio.modelo.TotalMensual;
import com.visual.backend.finanzas.dominio.puerto.ConsultaIndicadores;

/** Agregados del tablero financiero con SQL; solo cuentan los movimientos registrados. */
@Repository
public class ConsultaIndicadoresJdbc implements ConsultaIndicadores {

    private static final String DESDE_MOVIMIENTOS = """
            from movimientos_financieros m
            join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
            where m.estado = 'REGISTRADO' and m.fecha_movimiento between :desde and :hasta
            """;
    private static final String SUMAS = """
            select coalesce(sum(m.monto) filter (where t.naturaleza = 'INGRESO'), 0) as ingresos,
                   coalesce(sum(m.monto) filter (where t.naturaleza = 'EGRESO' and m.id_proyecto is not null), 0)
                       as costos_directos,
                   coalesce(sum(m.monto) filter (where t.naturaleza = 'EGRESO' and m.id_proyecto is null), 0)
                       as gastos_generales
            """ + DESDE_MOVIMIENTOS;
    private static final String MENSUALES = """
            select extract(year from m.fecha_movimiento)::int as anio,
                   extract(month from m.fecha_movimiento)::int as mes,
                   coalesce(sum(m.monto) filter (where t.naturaleza = 'INGRESO'), 0) as ingresos,
                   coalesce(sum(m.monto) filter (where t.naturaleza = 'EGRESO'), 0) as egresos
            """ + DESDE_MOVIMIENTOS + """
            group by anio, mes
            order by anio, mes
            """;
    private static final String DESTACADOS = """
            select m.id_movimiento, m.fecha_movimiento, t.nombre as tipo, t.naturaleza, m.concepto, m.monto,
                   p.nombre_proyecto
            from movimientos_financieros m
            join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
            left join proyectos p on p.id_proyecto = m.id_proyecto
            where m.estado = 'REGISTRADO' and m.fecha_movimiento between :desde and :hasta and m.monto >= :umbral
            order by m.monto desc, m.fecha_movimiento desc
            limit :limite
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaIndicadoresJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SumasPeriodo sumas(LocalDate desde, LocalDate hasta) {
        return jdbc.queryForObject(SUMAS, periodo(desde, hasta), (fila, n) -> new SumasPeriodo(
                fila.getBigDecimal("ingresos"), fila.getBigDecimal("costos_directos"),
                fila.getBigDecimal("gastos_generales")));
    }

    @Override
    public List<TotalMensual> totalesMensuales(LocalDate desde, LocalDate hasta) {
        return jdbc.query(MENSUALES, periodo(desde, hasta), (fila, n) -> new TotalMensual(
                YearMonth.of(fila.getInt("anio"), fila.getInt("mes")), fila.getBigDecimal("ingresos"),
                fila.getBigDecimal("egresos")));
    }

    @Override
    public List<MovimientoDestacado> destacados(LocalDate desde, LocalDate hasta, BigDecimal umbral, int limite) {
        MapSqlParameterSource parametros = periodo(desde, hasta).addValue("umbral", umbral).addValue("limite", limite);
        return jdbc.query(DESTACADOS, parametros, (fila, n) -> new MovimientoDestacado(fila.getInt("id_movimiento"),
                fila.getDate("fecha_movimiento").toLocalDate(), fila.getString("tipo"),
                Naturaleza.valueOf(fila.getString("naturaleza")), fila.getString("concepto"),
                fila.getBigDecimal("monto"), fila.getString("nombre_proyecto")));
    }

    private static MapSqlParameterSource periodo(LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource().addValue("desde", desde).addValue("hasta", hasta);
    }
}
