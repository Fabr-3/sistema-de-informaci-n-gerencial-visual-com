package com.visual.backend.finanzas.infraestructura.consultas;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.finanzas.dominio.modelo.DetalleResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.Filtros;
import com.visual.backend.finanzas.dominio.modelo.Margen;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.ResultadoEvento;
import com.visual.backend.finanzas.dominio.modelo.ResultadoProyecto;
import com.visual.backend.finanzas.dominio.modelo.TotalPorTipo;
import com.visual.backend.finanzas.dominio.puerto.ConsultaResultados;

/** Agregados de solo lectura con SQL; los movimientos anulados nunca se cuentan. */
@Repository
public class ConsultaResultadosJdbc implements ConsultaResultados {

    /** Sumas por naturaleza sobre el alias m (movimiento) y t (tipo). */
    private static final String SUMAS = """
            coalesce(sum(case when t.naturaleza = 'INGRESO' then m.monto end), 0) as ingresos,
            coalesce(sum(case when t.naturaleza = 'EGRESO' then m.monto end), 0) as costos
            """;
    private static final String MOVIMIENTOS_REGISTRADOS = """
            left join movimientos_financieros m on m.%s and m.estado = 'REGISTRADO'
            left join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
            """;
    private static final String DESDE_PROYECTOS = """
            from proyectos p
            join cotizaciones c on c.id_cotizacion = p.id_cotizacion
            join clientes cl on cl.id_cliente = c.id_cliente
            """;
    private static final String CONDICIONES_PROYECTOS = """
            where p.fecha_eliminacion is null
              and (cast(:patron as text) is null or lower(p.nombre_proyecto) like :patron escape '\\'
                   or lower(cl.razon_social) like :patron escape '\\')
              and (cast(:estado as text) is null or p.estado = :estado)
              and (cast(:idProyecto as integer) is null or p.id_proyecto = :idProyecto)
            """;
    /** Importe contratado de la cotizacion y sumas de los movimientos de cada proyecto. */
    private static final String PROYECTOS = """
            select p.id_proyecto, p.nombre_proyecto, cl.razon_social, p.estado,
                   coalesce(vi.importe_total, 0) as contratado,
                   coalesce(sm.ingresos, 0) as ingresos, coalesce(sm.costos, 0) as costos
            """ + DESDE_PROYECTOS + """
            left join v_cotizacion_importe vi on vi.id_cotizacion = p.id_cotizacion
            left join (select m.id_proyecto,
            """ + SUMAS + """
                       from movimientos_financieros m
                       join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
                       where m.estado = 'REGISTRADO' and m.id_proyecto is not null
                       group by m.id_proyecto) sm on sm.id_proyecto = p.id_proyecto
            """ + CONDICIONES_PROYECTOS + """
            order by p.fecha_inicio desc nulls last, p.id_proyecto desc
            limit :tamano offset :desplazamiento
            """;
    private static final String CONTAR_PROYECTOS = "select count(*) " + DESDE_PROYECTOS + CONDICIONES_PROYECTOS;
    private static final String EVENTOS = """
            select e.id_evento, e.nombre_evento, e.fecha_inicio, """ + SUMAS + """
            from eventos e
            """ + MOVIMIENTOS_REGISTRADOS.formatted("id_evento = e.id_evento") + """
            where e.id_proyecto = :idProyecto and e.fecha_eliminacion is null
            group by e.id_evento
            order by e.fecha_inicio
            """;
    private static final String SIN_EVENTO = "select " + SUMAS + """
            from movimientos_financieros m
            join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
            where m.id_proyecto = :idProyecto and m.id_evento is null and m.estado = 'REGISTRADO'
            """;
    private static final String POR_TIPO = """
            select t.nombre, t.naturaleza, sum(m.monto) as monto
            from movimientos_financieros m
            join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
            where m.id_proyecto = :idProyecto and m.estado = 'REGISTRADO'
            group by t.nombre, t.naturaleza
            order by t.naturaleza desc, monto desc
            """;
    private static final String ETIQUETA_SIN_EVENTO = "Sin evento (todo el proyecto)";

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaResultadosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<ResultadoProyecto> buscarProyectos(Filtros.ResultadosProyectos filtro, SolicitudPagina solicitud) {
        MapSqlParameterSource parametros = parametrosProyectos(filtro.texto(), filtro.estado(), null)
                .addValue("tamano", solicitud.tamano())
                .addValue("desplazamiento", (long) solicitud.pagina() * solicitud.tamano());
        List<ResultadoProyecto> contenido = jdbc.query(PROYECTOS, parametros, (fila, n) -> proyecto(fila));
        Long total = jdbc.queryForObject(CONTAR_PROYECTOS, parametros, Long.class);
        long totalElementos = total == null ? 0 : total;
        int totalPaginas = (int) Math.ceil((double) totalElementos / solicitud.tamano());
        return new Pagina<>(contenido, solicitud.pagina(), solicitud.tamano(), totalElementos, totalPaginas);
    }

    @Override
    public Optional<DetalleResultadoProyecto> detalleProyecto(Integer idProyecto) {
        MapSqlParameterSource parametros = parametrosProyectos(null, null, idProyecto)
                .addValue("tamano", 1).addValue("desplazamiento", 0);
        List<ResultadoProyecto> proyectos = jdbc.query(PROYECTOS, parametros, (fila, n) -> proyecto(fila));
        if (proyectos.isEmpty()) {
            return Optional.empty();
        }
        List<ResultadoEvento> eventos = new ArrayList<>(jdbc.query(EVENTOS, parametros,
                (fila, n) -> new ResultadoEvento(fila.getInt("id_evento"), fila.getString("nombre_evento"),
                        fila.getTimestamp("fecha_inicio").toLocalDateTime(), margen(fila))));
        Margen sinEvento = jdbc.queryForObject(SIN_EVENTO, parametros, (fila, n) -> margen(fila));
        if (sinEvento != null && (sinEvento.ingresos().signum() != 0 || sinEvento.costos().signum() != 0)) {
            eventos.add(new ResultadoEvento(null, ETIQUETA_SIN_EVENTO, null, sinEvento));
        }
        List<TotalPorTipo> porTipo = jdbc.query(POR_TIPO, parametros, (fila, n) -> new TotalPorTipo(
                fila.getString("nombre"), Naturaleza.valueOf(fila.getString("naturaleza")), fila.getBigDecimal("monto")));
        return Optional.of(new DetalleResultadoProyecto(proyectos.get(0), List.copyOf(eventos), porTipo));
    }

    private static MapSqlParameterSource parametrosProyectos(String texto, String estado, Integer idProyecto) {
        String patron = texto == null || texto.isBlank() ? null : ConsultasJpa.patronContiene(texto);
        return new MapSqlParameterSource()
                .addValue("patron", patron)
                .addValue("estado", estado == null || estado.isBlank() ? null : estado)
                .addValue("idProyecto", idProyecto);
    }

    private static ResultadoProyecto proyecto(ResultSet fila) throws SQLException {
        return ResultadoProyecto.de(fila.getInt("id_proyecto"), fila.getString("nombre_proyecto"),
                fila.getString("razon_social"), fila.getString("estado"), fila.getBigDecimal("contratado"),
                fila.getBigDecimal("ingresos"), fila.getBigDecimal("costos"));
    }

    private static Margen margen(ResultSet fila) throws SQLException {
        return Margen.calcular(valor(fila.getBigDecimal("ingresos")), valor(fila.getBigDecimal("costos")));
    }

    private static BigDecimal valor(BigDecimal numero) {
        return numero == null ? BigDecimal.ZERO : numero;
    }
}
