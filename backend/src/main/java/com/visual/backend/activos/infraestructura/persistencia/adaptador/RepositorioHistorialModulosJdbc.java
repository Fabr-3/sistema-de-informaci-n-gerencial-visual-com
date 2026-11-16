package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.EntradaHistorialModulo;
import com.visual.backend.activos.dominio.modelo.EstadoModuloLed;
import com.visual.backend.activos.dominio.modelo.HistorialModuloLed;
import com.visual.backend.activos.dominio.modelo.TipoCambioModulo;
import com.visual.backend.activos.dominio.puerto.RepositorioHistorialModulos;

/** El historial solo se inserta y se consulta: se maneja con SQL directo. */
@Repository
public class RepositorioHistorialModulosJdbc implements RepositorioHistorialModulos {

    private static final String INSERTAR = """
            insert into historial_modulos_led (id_modulo, tipo, id_activo_origen, id_activo_destino,
                                               estado_anterior, estado_nuevo, id_modulo_relacionado, motivo,
                                               fecha, registrado_por)
            values (:idModulo, :tipo, :idOrigen, :idDestino, :estadoAnterior, :estadoNuevo, :idRelacionado,
                    :motivo, :fecha, :registradoPor)
            """;
    private static final String HISTORIAL = """
            select h.tipo, o.codigo_inventario as origen, d.codigo_inventario as destino, h.estado_anterior,
                   h.estado_nuevo, r.codigo_modulo as relacionado, h.motivo, h.fecha,
                   trim(u.nombres || ' ' || u.apellido_paterno) as registrado_por
            from historial_modulos_led h
            left join activos o on o.id_activo = h.id_activo_origen
            left join activos d on d.id_activo = h.id_activo_destino
            left join modulos_led r on r.id_modulo = h.id_modulo_relacionado
            left join usuarios u on u.id_usuario = h.registrado_por
            where h.id_modulo = :idModulo
            order by h.fecha, h.id_historial
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioHistorialModulosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(HistorialModuloLed cambio) {
        jdbc.update(INSERTAR, new MapSqlParameterSource()
                .addValue("idModulo", cambio.idModulo())
                .addValue("tipo", cambio.tipo().name())
                .addValue("idOrigen", cambio.idActivoOrigen())
                .addValue("idDestino", cambio.idActivoDestino())
                .addValue("estadoAnterior", nombre(cambio.estadoAnterior()))
                .addValue("estadoNuevo", nombre(cambio.estadoNuevo()))
                .addValue("idRelacionado", cambio.idModuloRelacionado())
                .addValue("motivo", cambio.motivo())
                .addValue("fecha", cambio.fecha())
                .addValue("registradoPor", cambio.registradoPor()));
    }

    @Override
    public List<EntradaHistorialModulo> historial(Integer idModulo) {
        return jdbc.query(HISTORIAL, new MapSqlParameterSource("idModulo", idModulo),
                RepositorioHistorialModulosJdbc::entrada);
    }

    private static EntradaHistorialModulo entrada(ResultSet fila, int numero) throws SQLException {
        return new EntradaHistorialModulo(TipoCambioModulo.valueOf(fila.getString("tipo")), fila.getString("origen"),
                fila.getString("destino"), estado(fila.getString("estado_anterior")),
                estado(fila.getString("estado_nuevo")), fila.getString("relacionado"), fila.getString("motivo"),
                fila.getObject("fecha", LocalDateTime.class), fila.getString("registrado_por"));
    }

    private static String nombre(EstadoModuloLed estado) {
        return estado == null ? null : estado.name();
    }

    private static EstadoModuloLed estado(String valor) {
        return valor == null ? null : EstadoModuloLed.valueOf(valor);
    }
}
