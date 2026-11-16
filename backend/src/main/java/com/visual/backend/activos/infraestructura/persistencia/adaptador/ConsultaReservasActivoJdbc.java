package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.ReservaVigenteActivo;
import com.visual.backend.activos.dominio.puerto.ConsultaReservasActivo;

@Repository
public class ConsultaReservasActivoJdbc implements ConsultaReservasActivo {

    private static final String VIGENTES = """
            select r.id_reserva, e.id_evento, e.nombre_evento, r.fecha_salida, r.fecha_retorno
            from reservas_activo r
            join planes_logisticos p on p.id_plan = r.id_plan
            join eventos e on e.id_evento = p.id_evento
            where r.id_activo = :idActivo and r.estado = 'ACTIVA' and r.fecha_retorno > :desde
            order by r.fecha_salida
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReservasActivoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReservaVigenteActivo> vigentes(Integer idActivo, LocalDateTime desde) {
        return jdbc.query(VIGENTES, new MapSqlParameterSource().addValue("idActivo", idActivo)
                .addValue("desde", desde), ConsultaReservasActivoJdbc::reserva);
    }

    private static ReservaVigenteActivo reserva(ResultSet fila, int numero) throws SQLException {
        return new ReservaVigenteActivo(fila.getInt("id_reserva"), fila.getInt("id_evento"),
                fila.getString("nombre_evento"), fila.getObject("fecha_salida", LocalDateTime.class),
                fila.getObject("fecha_retorno", LocalDateTime.class));
    }
}
