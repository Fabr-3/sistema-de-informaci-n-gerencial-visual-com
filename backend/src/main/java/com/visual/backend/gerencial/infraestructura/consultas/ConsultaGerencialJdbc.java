package com.visual.backend.gerencial.infraestructura.consultas;

import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.ACTIVO_CON_MODELO;
import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.ACTIVO_VIGENTE;

import java.util.List;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.gerencial.dominio.modelo.Periodo;
import com.visual.backend.gerencial.dominio.modelo.ResultadoCliente;
import com.visual.backend.gerencial.dominio.modelo.TasaUtilizacion;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionCategoria;
import com.visual.backend.gerencial.dominio.modelo.UtilizacionPersonal;
import com.visual.backend.gerencial.dominio.puerto.ConsultaGerencial;

/**
 * Agregados de los KPIs gerenciales con SQL. Los dias ocupados se cuentan
 * una sola vez por recurso aunque tenga varios usos o asignaciones ese dia;
 * un uso que termina exactamente a medianoche no ocupa el dia siguiente.
 */
@Repository
public class ConsultaGerencialJdbc implements ConsultaGerencial {

    private static final String RESULTADOS_POR_CLIENTE = """
            with por_proyecto as (
                select p.id_proyecto, c.id_cliente,
                       coalesce(sum(m.monto) filter (where t.naturaleza = 'INGRESO'), 0) as ingresos,
                       coalesce(sum(m.monto) filter (where t.naturaleza = 'EGRESO'), 0) as costos
                from movimientos_financieros m
                join tipos_movimiento t on t.id_tipo_movimiento = m.id_tipo_movimiento
                join proyectos p on p.id_proyecto = m.id_proyecto
                join cotizaciones c on c.id_cotizacion = p.id_cotizacion
                where m.estado = 'REGISTRADO' and m.fecha_movimiento between :desde and :hasta
                group by p.id_proyecto, c.id_cliente
            ), contratados as (
                select c.id_cliente, count(*) as proyectos
                from proyectos p
                join cotizaciones c on c.id_cotizacion = p.id_cotizacion
                where p.estado <> 'CANCELADO' and p.fecha_eliminacion is null
                  and p.fecha_creacion < cast(:hasta as date) + 1
                group by c.id_cliente
            )
            select cl.id_cliente, cl.razon_social, coalesce(k.proyectos, 0) as contratados,
                   count(*) as con_movimientos, sum(pp.ingresos) as ingresos, sum(pp.costos) as costos
            from por_proyecto pp
            join clientes cl on cl.id_cliente = pp.id_cliente
            left join contratados k on k.id_cliente = pp.id_cliente
            group by cl.id_cliente, cl.razon_social, k.proyectos
            order by cl.razon_social
            """;
    private static final String UTILIZACION_EQUIPOS = """
            with equipos as (
                select a.id_activo, c.nombre as categoria,
                       greatest(cast(:desde as date), coalesce(a.fecha_adquisicion, a.fecha_creacion::date)) as alta
                from activos a
            """ + ACTIVO_CON_MODELO + """
                where %s and mo.tipo_control = 'UNIDAD'
                  and coalesce(a.fecha_adquisicion, a.fecha_creacion::date) <= :hasta
            ), ocupados as (
                select e.id_activo, count(distinct dia::date) as dias
                from equipos e
                join registros_uso r on r.id_activo = e.id_activo and r.fecha_eliminacion is null
                cross join lateral generate_series(greatest(r.fecha_inicio::date, e.alta),
                        least((r.fecha_fin - interval '1 microsecond')::date, cast(:hasta as date)),
                        interval '1 day') as dia
                where r.fecha_fin > :desde and r.fecha_inicio < cast(:hasta as date) + 1
                group by e.id_activo
            )
            select e.categoria, count(*) as recursos, sum(cast(:hasta as date) - e.alta + 1) as disponibles,
                   coalesce(sum(o.dias), 0) as ocupados
            from equipos e
            left join ocupados o on o.id_activo = e.id_activo
            group by e.categoria
            order by e.categoria
            """.formatted(ACTIVO_VIGENTE);
    private static final String UTILIZACION_PERSONAL = """
            with asignaciones as (
                select ap.id_personal, pl.id_evento,
                       greatest(ap.fecha_inicio_actividad, cast(:desde as timestamp)) as inicio,
                       least(ap.fecha_fin_actividad, cast(:hasta as date) + interval '1 day') as fin
                from asignaciones_personal ap
                join planes_logisticos pl on pl.id_plan = ap.id_plan and pl.fecha_eliminacion is null
                join eventos e on e.id_evento = pl.id_evento
                where e.estado <> 'CANCELADO' and e.fecha_eliminacion is null
                  and ap.fecha_fin_actividad > :desde and ap.fecha_inicio_actividad < cast(:hasta as date) + 1
            ), dias as (
                select id_personal, count(distinct dia::date) as dias
                from asignaciones
                cross join lateral generate_series(inicio::date, (fin - interval '1 microsecond')::date,
                        interval '1 day') as dia
                group by id_personal
            ), totales as (
                select id_personal, count(distinct id_evento) as eventos,
                       sum(extract(epoch from (fin - inicio)) / 3600) as horas
                from asignaciones
                group by id_personal
            )
            select p.id_personal, p.nombres || ' ' || p.apellidos as nombre, coalesce(d.dias, 0) as dias,
                   coalesce(t.eventos, 0) as eventos, coalesce(t.horas, 0)::numeric(12,1) as horas
            from personal_tecnico p
            left join dias d on d.id_personal = p.id_personal
            left join totales t on t.id_personal = p.id_personal
            where p.fecha_eliminacion is null and p.activo
            order by nombre
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaGerencialJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ResultadoCliente> resultadosPorCliente(Periodo periodo) {
        return jdbc.query(RESULTADOS_POR_CLIENTE, FragmentosSql.periodo(periodo), (fila, n) -> ResultadoCliente.de(
                fila.getInt("id_cliente"), fila.getString("razon_social"), fila.getInt("contratados"),
                fila.getInt("con_movimientos"), fila.getBigDecimal("ingresos"), fila.getBigDecimal("costos")));
    }

    @Override
    public List<UtilizacionCategoria> utilizacionEquipos(Periodo periodo) {
        return jdbc.query(UTILIZACION_EQUIPOS, FragmentosSql.periodo(periodo), (fila, n) -> new UtilizacionCategoria(
                fila.getString("categoria"), TasaUtilizacion.de(fila.getInt("recursos"), fila.getLong("disponibles"),
                        fila.getLong("ocupados"))));
    }

    /** Cada tecnico activo dispone de todos los dias del periodo. */
    @Override
    public List<UtilizacionPersonal> utilizacionPersonal(Periodo periodo) {
        return jdbc.query(UTILIZACION_PERSONAL, FragmentosSql.periodo(periodo), (fila, n) -> new UtilizacionPersonal(
                fila.getInt("id_personal"), fila.getString("nombre"),
                TasaUtilizacion.de(1, periodo.dias(), fila.getLong("dias")), fila.getInt("eventos"),
                fila.getBigDecimal("horas")));
    }
}
