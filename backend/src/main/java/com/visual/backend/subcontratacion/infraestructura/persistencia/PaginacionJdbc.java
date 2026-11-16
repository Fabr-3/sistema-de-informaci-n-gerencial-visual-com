package com.visual.backend.subcontratacion.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;

/** Paginacion de consultas SQL: la consulta de datos termina en "limit :tamano offset :desplazamiento". */
final class PaginacionJdbc {

    static final String LIMITE = " limit :tamano offset :desplazamiento";

    private PaginacionJdbc() {
    }

    static <T> Pagina<T> paginar(NamedParameterJdbcTemplate jdbc, String consulta, String conteo,
                                 MapSqlParameterSource parametros, SolicitudPagina solicitud, RowMapper<T> filas) {
        parametros.addValue("tamano", solicitud.tamano())
                .addValue("desplazamiento", (long) solicitud.pagina() * solicitud.tamano());
        List<T> contenido = jdbc.query(consulta + LIMITE, parametros, filas);
        Long total = jdbc.queryForObject(conteo, parametros, Long.class);
        long totalElementos = total == null ? 0 : total;
        int totalPaginas = (int) Math.ceil((double) totalElementos / solicitud.tamano());
        return new Pagina<>(contenido, solicitud.pagina(), solicitud.tamano(), totalElementos, totalPaginas);
    }

    /** Valor de un id opcional leido de una columna que puede ser nula. */
    static Integer entero(ResultSet fila, String columna) throws SQLException {
        int valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }
}
