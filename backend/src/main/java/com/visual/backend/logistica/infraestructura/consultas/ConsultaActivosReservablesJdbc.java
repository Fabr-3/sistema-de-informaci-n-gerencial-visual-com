package com.visual.backend.logistica.infraestructura.consultas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;
import com.visual.backend.logistica.dominio.puerto.ConsultaActivosReservables;

/** Lee los activos con SQL de solo lectura, sin depender de las entidades del modulo de activos. */
@Repository
public class ConsultaActivosReservablesJdbc implements ConsultaActivosReservables {

    /** Evita listas enormes en el dialogo de reserva; el usuario acota con el texto o la categoria. */
    static final int MAXIMO_DISPONIBLES = 200;

    private static final String SELECCION = """
            select a.id_activo, a.codigo_inventario, f.nombre || ' ' || mo.nombre as modelo, c.nombre as categoria,
                   mo.tipo_control = 'UNIDAD' as por_unidad,
                   a.estado_operativo not in ('BAJA', 'VENDIDO') as vigente,
                   a.estado_operativo = 'EN_MANTENIMIENTO' or exists (
                       select 1 from mantenimientos m where m.id_activo = a.id_activo
                       and m.estado = 'EN_PROCESO' and m.fecha_eliminacion is null) as en_mantenimiento
            from activos a
            join modelos_activo mo on mo.id_modelo = a.id_modelo
            join fabricantes f on f.id_fabricante = mo.id_fabricante
            join categorias_activo c on c.id_categoria = mo.id_categoria
            where a.fecha_eliminacion is null
            """;
    private static final String POR_IDS = SELECCION + " and a.id_activo in (:ids) order by a.codigo_inventario";
    private static final String DISPONIBLES = "select * from (" + SELECCION + """
              and (cast(:idCategoria as integer) is null or mo.id_categoria = :idCategoria)
              and (cast(:patron as varchar) is null or lower(a.codigo_inventario) like :patron
                   or lower(coalesce(a.numero_serie, '')) like :patron
                   or lower(f.nombre || ' ' || mo.nombre) like :patron)
              and not exists (select 1 from reservas_activo r where r.id_activo = a.id_activo
                              and r.estado = 'ACTIVA' and r.fecha_salida < :retorno and r.fecha_retorno > :salida)
            ) candidatos
            where por_unidad and vigente and not en_mantenimiento
            order by categoria, codigo_inventario
            limit :maximo
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaActivosReservablesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ActivoReservable> buscarPorIds(Collection<Integer> idsActivos) {
        if (idsActivos.isEmpty()) {
            return List.of();
        }
        return jdbc.query(POR_IDS, new MapSqlParameterSource("ids", idsActivos),
                ConsultaActivosReservablesJdbc::activo);
    }

    @Override
    public List<ActivoReservable> buscarDisponibles(PeriodoReserva periodo, Filtros.ActivosReservables filtro) {
        String texto = filtro.texto();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("salida", periodo.salida())
                .addValue("retorno", periodo.retorno())
                .addValue("idCategoria", filtro.idCategoria())
                .addValue("patron", texto == null || texto.isBlank() ? null : ConsultasJpa.patronContiene(texto))
                .addValue("maximo", MAXIMO_DISPONIBLES);
        return jdbc.query(DISPONIBLES, parametros, ConsultaActivosReservablesJdbc::activo);
    }

    private static ActivoReservable activo(ResultSet fila, int numero) throws SQLException {
        return new ActivoReservable(fila.getInt("id_activo"), fila.getString("codigo_inventario"),
                fila.getString("modelo"), fila.getString("categoria"), fila.getBoolean("por_unidad"),
                fila.getBoolean("vigente"), fila.getBoolean("en_mantenimiento"));
    }
}
