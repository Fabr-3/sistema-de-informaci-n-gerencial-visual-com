package com.visual.backend.gerencial.infraestructura.consultas;

import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.ACTIVO_CON_MODELO;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.gerencial.dominio.modelo.FilaEvento;
import com.visual.backend.gerencial.dominio.modelo.FilaInventario;
import com.visual.backend.gerencial.dominio.modelo.FilaLote;
import com.visual.backend.gerencial.dominio.modelo.FilaMantenimiento;
import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.puerto.ConsultaReportes;

/** Datos de los reportes operativos con SQL. */
@Repository
public class ConsultaReportesJdbc implements ConsultaReportes {

    private static final String EVENTOS = """
            select e.fecha_inicio, e.fecha_fin, e.nombre_evento, p.nombre_proyecto, cl.razon_social,
                   l.nombre || coalesce(' (' || l.ciudad || ')', '') as lugar, e.estado,
                   (select count(distinct ap.id_personal)
                    from planes_logisticos pl join asignaciones_personal ap on ap.id_plan = pl.id_plan
                    where pl.id_evento = e.id_evento and pl.fecha_eliminacion is null) as personal,
                   (select count(*)
                    from planes_logisticos pl join jornadas_logisticas j on j.id_plan = pl.id_plan
                    where pl.id_evento = e.id_evento and pl.fecha_eliminacion is null) as jornadas
            from eventos e
            join proyectos p on p.id_proyecto = e.id_proyecto
            join cotizaciones c on c.id_cotizacion = p.id_cotizacion
            join clientes cl on cl.id_cliente = c.id_cliente
            join lugares l on l.id_lugar = e.id_lugar
            where e.fecha_eliminacion is null and p.fecha_eliminacion is null
              and e.fecha_fin > :desde and e.fecha_inicio < cast(:hasta as date) + 1
            order by e.fecha_inicio, e.nombre_evento
            """;
    private static final String MANTENIMIENTOS = """
            select a.codigo_inventario, f.nombre || ' ' || mo.nombre as modelo, tm.nombre as tipo,
                   m.fecha_inicio, m.fecha_fin, m.estado, m.condicion_final, m.costo
            from mantenimientos m
            join tipos_mantenimiento tm on tm.id_tipo_mantenimiento = m.id_tipo_mantenimiento
            join activos a on a.id_activo = m.id_activo
            """ + ACTIVO_CON_MODELO + """
            where m.fecha_eliminacion is null and m.fecha_inicio < cast(:hasta as date) + 1
              and (m.fecha_fin is null or m.fecha_fin >= :desde)
            order by m.fecha_inicio, a.codigo_inventario
            """;
    private static final String INVENTARIO = """
            select c.nombre as categoria, count(*) as total,
                   count(*) filter (where a.estado_operativo = 'DISPONIBLE') as disponibles,
                   count(*) filter (where a.estado_operativo in ('RESERVADO', 'EN_USO')) as en_uso,
                   count(*) filter (where a.estado_operativo = 'EN_MANTENIMIENTO') as en_mantenimiento,
                   count(*) filter (where a.estado_operativo = 'BAJA') as de_baja,
                   count(*) filter (where a.estado_operativo = 'VENDIDO') as vendidos,
                   coalesce(sum(a.costo_adquisicion)
                            filter (where a.estado_operativo not in ('BAJA', 'VENDIDO')), 0) as valor
            from activos a
            """ + ACTIVO_CON_MODELO + """
            where a.fecha_eliminacion is null and mo.tipo_control = 'UNIDAD'
            group by c.nombre
            order by c.nombre
            """;
    private static final String LOTES = """
            select f.nombre || ' ' || mo.nombre as modelo, c.nombre as categoria, b.nombre as bodega, ex.cantidad
            from existencias_lote ex
            join modelos_activo mo on mo.id_modelo = ex.id_modelo
            join fabricantes f on f.id_fabricante = mo.id_fabricante
            join categorias_activo c on c.id_categoria = mo.id_categoria
            join bodegas b on b.id_bodega = ex.id_bodega
            where ex.cantidad > 0
            order by c.nombre, modelo, b.nombre
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReportesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FilaEvento> eventos(Periodo periodo) {
        return jdbc.query(EVENTOS, FragmentosSql.periodo(periodo), (fila, n) -> new FilaEvento(
                fecha(fila.getTimestamp("fecha_inicio")), fecha(fila.getTimestamp("fecha_fin")),
                fila.getString("nombre_evento"), fila.getString("nombre_proyecto"), fila.getString("razon_social"),
                fila.getString("lugar"), fila.getString("estado"), fila.getInt("personal"), fila.getInt("jornadas")));
    }

    @Override
    public List<FilaMantenimiento> mantenimientos(Periodo periodo) {
        return jdbc.query(MANTENIMIENTOS, FragmentosSql.periodo(periodo), (fila, n) -> new FilaMantenimiento(
                fila.getString("codigo_inventario"), fila.getString("modelo"), fila.getString("tipo"),
                fecha(fila.getTimestamp("fecha_inicio")), fecha(fila.getTimestamp("fecha_fin")),
                fila.getString("estado"), fila.getString("condicion_final"), fila.getBigDecimal("costo")));
    }

    @Override
    public List<FilaInventario> inventario() {
        return jdbc.query(INVENTARIO, new MapSqlParameterSource(), (fila, n) -> new FilaInventario(
                fila.getString("categoria"), fila.getInt("total"), fila.getInt("disponibles"), fila.getInt("en_uso"),
                fila.getInt("en_mantenimiento"), fila.getInt("de_baja"), fila.getInt("vendidos"),
                fila.getBigDecimal("valor")));
    }

    @Override
    public List<FilaLote> lotes() {
        return jdbc.query(LOTES, new MapSqlParameterSource(), (fila, n) -> new FilaLote(fila.getString("modelo"),
                fila.getString("categoria"), fila.getString("bodega"), fila.getInt("cantidad")));
    }

    private static LocalDateTime fecha(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }
}
