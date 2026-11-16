package com.visual.backend.comercial.infraestructura.consultas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.ResumenComercialCliente;
import com.visual.backend.comercial.dominio.modelo.ResumenComercialCliente.Reciente;
import com.visual.backend.comercial.dominio.puerto.ConsultaResumenCliente;

/** Agregados de solo lectura con SQL sobre las cotizaciones y proyectos del cliente. */
@Repository
public class ConsultaResumenClienteJdbc implements ConsultaResumenCliente {

    private static final String COTIZACIONES = """
            from cotizaciones c where c.id_cliente = :idCliente and c.fecha_eliminacion is null
            """;
    private static final String COTIZACIONES_POR_ESTADO = "select c.estado, count(*) as cantidad " + COTIZACIONES
            + " group by c.estado order by c.estado";
    private static final String IMPORTES = """
            select coalesce(sum(vi.importe_total), 0) as cotizado,
                   coalesce(sum(vi.importe_total) filter (where c.estado = 'ACEPTADA'), 0) as contratado
            from cotizaciones c
            join v_cotizacion_importe vi on vi.id_cotizacion = c.id_cotizacion
            where c.id_cliente = :idCliente and c.fecha_eliminacion is null
            """;
    private static final String ULTIMAS_COTIZACIONES = "select c.id_cotizacion as id, c.numero as referencia, "
            + "c.estado, c.fecha_emision as fecha " + COTIZACIONES
            + " order by c.fecha_emision desc, c.id_cotizacion desc limit :limite";
    private static final String PROYECTOS = """
            from proyectos p
            join cotizaciones c on c.id_cotizacion = p.id_cotizacion
            where c.id_cliente = :idCliente and p.fecha_eliminacion is null
            """;
    private static final String PROYECTOS_POR_ESTADO = "select p.estado, count(*) as cantidad " + PROYECTOS
            + " group by p.estado order by p.estado";
    private static final String ULTIMOS_PROYECTOS = "select p.id_proyecto as id, p.nombre_proyecto as referencia, "
            + "p.estado, p.fecha_inicio as fecha " + PROYECTOS
            + " order by p.fecha_inicio desc nulls last, p.id_proyecto desc limit :limite";

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaResumenClienteJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ResumenComercialCliente resumen(Integer idCliente, int limiteRecientes) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("idCliente", idCliente)
                .addValue("limite", limiteRecientes);
        Map<String, BigDecimal> importes = jdbc.queryForObject(IMPORTES, parametros, (fila, n) ->
                Map.of("cotizado", fila.getBigDecimal("cotizado"), "contratado", fila.getBigDecimal("contratado")));
        return new ResumenComercialCliente(idCliente, porEstado(COTIZACIONES_POR_ESTADO, parametros),
                importes.get("cotizado"), importes.get("contratado"), porEstado(PROYECTOS_POR_ESTADO, parametros),
                recientes(ULTIMAS_COTIZACIONES, parametros), recientes(ULTIMOS_PROYECTOS, parametros));
    }

    private Map<String, Long> porEstado(String sql, MapSqlParameterSource parametros) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        jdbc.query(sql, parametros, fila -> {
            conteo.put(fila.getString("estado"), fila.getLong("cantidad"));
        });
        return conteo;
    }

    private List<Reciente> recientes(String sql, MapSqlParameterSource parametros) {
        return jdbc.query(sql, parametros, (fila, n) -> new Reciente(fila.getInt("id"), fila.getString("referencia"),
                fila.getString("estado"), fila.getObject("fecha", LocalDate.class)));
    }
}
