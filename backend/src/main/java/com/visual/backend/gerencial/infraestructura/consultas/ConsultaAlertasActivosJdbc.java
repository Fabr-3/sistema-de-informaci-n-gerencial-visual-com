package com.visual.backend.gerencial.infraestructura.consultas;

import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.ACTIVO_CON_MODELO;
import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.ACTIVO_VIGENTE;
import static com.visual.backend.gerencial.infraestructura.consultas.FragmentosSql.COLUMNAS_ACTIVO;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.aplicacion.dto.VistaActivo;
import com.visual.backend.activos.aplicacion.servicio.ServicioActivos;
import com.visual.backend.activos.dominio.modelo.NivelAlertaMantenimiento;
import com.visual.backend.gerencial.dominio.modelo.ActivoAfectado;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.puerto.ConsultaAlertasActivos;

/**
 * Situaciones de alerta de los activos vigentes. El mantenimiento preventivo
 * se toma del modulo de activos, que ya calcula las horas y su umbral.
 */
@Repository
public class ConsultaAlertasActivosJdbc implements ConsultaAlertasActivos {

    private static final String DESDE_ACTIVOS = " from activos a " + ACTIVO_CON_MODELO;
    private static final String PROLONGADOS = "select " + COLUMNAS_ACTIVO + ", m.fecha_inicio::date as inicio"
            + DESDE_ACTIVOS + """
            join mantenimientos m on m.id_activo = a.id_activo
            where a.fecha_eliminacion is null and m.estado = 'EN_PROCESO' and m.fecha_eliminacion is null
              and m.fecha_inicio < cast(:limite as date) + 1
            order by m.fecha_inicio
            """;
    private static final String MODULOS_DEFECTUOSOS = "select " + COLUMNAS_ACTIVO + ", count(*) as cantidad"
            + DESDE_ACTIVOS + """
            join modulos_led ml on ml.id_activo_padre = a.id_activo
            where %s and ml.estado = 'DEFECTUOSO' and ml.fecha_eliminacion is null
            group by a.id_activo, a.codigo_inventario, f.nombre, mo.nombre, c.nombre
            """.formatted(ACTIVO_VIGENTE);
    private static final String FALLAS_RECURRENTES = "select " + COLUMNAS_ACTIVO + ", count(*) as cantidad"
            + DESDE_ACTIVOS + """
            join mantenimientos m on m.id_activo = a.id_activo
            join tipos_mantenimiento tm on tm.id_tipo_mantenimiento = m.id_tipo_mantenimiento
            where %s and not tm.reinicia_horas and m.fecha_eliminacion is null and m.fecha_inicio >= :desde
            group by a.id_activo, a.codigo_inventario, f.nombre, mo.nombre, c.nombre
            having count(*) >= :minimo
            """.formatted(ACTIVO_VIGENTE);
    private static final String SIN_USO = "select " + COLUMNAS_ACTIVO + ", max(r.fecha_fin)::date as ultimo_uso"
            + DESDE_ACTIVOS + """
            left join registros_uso r on r.id_activo = a.id_activo and r.fecha_eliminacion is null
            where a.fecha_eliminacion is null and a.estado_operativo = 'DISPONIBLE' and mo.tipo_control = 'UNIDAD'
              and coalesce(a.fecha_adquisicion, a.fecha_creacion::date) < :desde
            group by a.id_activo, a.codigo_inventario, f.nombre, mo.nombre, c.nombre
            having max(r.fecha_fin) is null or max(r.fecha_fin) < :desde
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ServicioActivos activos;

    public ConsultaAlertasActivosJdbc(NamedParameterJdbcTemplate jdbc, ServicioActivos activos) {
        this.jdbc = jdbc;
        this.activos = activos;
    }

    @Override
    public List<AlertaActivo> mantenimientoPreventivo() {
        return activos.listarAlertasMantenimiento().stream()
                .filter(activo -> activo.nivelAlerta().requiereAtencion())
                .map(activo -> AlertaActivo.mantenimientoPreventivo(afectado(activo),
                        activo.nivelAlerta() == NivelAlertaMantenimiento.VENCIDO, activo.horasDesdeMantenimiento(),
                        activo.horasMantenimiento()))
                .toList();
    }

    @Override
    public List<AlertaActivo> mantenimientosProlongados(LocalDate iniciadosHasta, LocalDate hoy) {
        return jdbc.query(PROLONGADOS, new MapSqlParameterSource("limite", iniciadosHasta), (fila, n) ->
                AlertaActivo.mantenimientoProlongado(afectado(fila), fila.getDate("inicio").toLocalDate(), hoy));
    }

    @Override
    public List<AlertaActivo> modulosDefectuosos() {
        return jdbc.query(MODULOS_DEFECTUOSOS, new MapSqlParameterSource(), (fila, n) ->
                AlertaActivo.modulosDefectuosos(afectado(fila), fila.getInt("cantidad")));
    }

    @Override
    public List<AlertaActivo> fallasRecurrentes(int minimoCorrectivos, LocalDate desde) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("desde", desde)
                .addValue("minimo", minimoCorrectivos);
        return jdbc.query(FALLAS_RECURRENTES, parametros, (fila, n) ->
                AlertaActivo.fallasRecurrentes(afectado(fila), fila.getInt("cantidad")));
    }

    @Override
    public List<AlertaActivo> sinUso(LocalDate desde, LocalDate hoy) {
        return jdbc.query(SIN_USO, new MapSqlParameterSource("desde", desde), (fila, n) -> {
            Date ultimoUso = fila.getDate("ultimo_uso");
            return AlertaActivo.sinUso(afectado(fila), ultimoUso == null ? null : ultimoUso.toLocalDate(), hoy);
        });
    }

    private static ActivoAfectado afectado(VistaActivo activo) {
        return new ActivoAfectado(activo.id(), activo.codigoInventario(), activo.modelo(), activo.categoria());
    }

    private static ActivoAfectado afectado(ResultSet fila) throws SQLException {
        return new ActivoAfectado(fila.getInt("id_activo"), fila.getString("codigo_inventario"),
                fila.getString("modelo"), fila.getString("categoria"));
    }
}
