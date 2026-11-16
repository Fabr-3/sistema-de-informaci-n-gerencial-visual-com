package com.visual.backend.gerencial.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.gerencial.dominio.modelo.Alerta;
import com.visual.backend.gerencial.dominio.modelo.EstadoAlerta;
import com.visual.backend.gerencial.dominio.modelo.FiltroAlertas;
import com.visual.backend.gerencial.dominio.modelo.Severidad;
import com.visual.backend.gerencial.dominio.modelo.TipoAlerta;
import com.visual.backend.gerencial.dominio.puerto.RepositorioAlertas;

/** Alertas con SQL; se leen junto con los datos del activo y de quien las atendio. */
@Repository
public class RepositorioAlertasJdbc implements RepositorioAlertas {

    /** Las alertas de stock no tienen activo: sus datos de equipo quedan nulos. */
    private static final String DESDE = """
            select al.*, a.codigo_inventario, f.nombre || ' ' || mo.nombre as modelo, c.nombre as categoria,
                   trim(u.nombres || ' ' || u.apellido_paterno) as nombre_atendio
            from alertas al
            left join activos a on a.id_activo = al.id_activo
            left join modelos_activo mo on mo.id_modelo = a.id_modelo
            left join fabricantes f on f.id_fabricante = mo.id_fabricante
            left join categorias_activo c on c.id_categoria = mo.id_categoria
            left join usuarios u on u.id_usuario = al.atendida_por
            """;
    private static final String ABIERTAS = DESDE + " where al.estado <> 'RESUELTA'";
    private static final String BUSCAR = DESDE + """
            where (cast(:estado as varchar) is null or al.estado = :estado)
              and (cast(:tipo as varchar) is null or al.tipo = :tipo)
              and (cast(:severidad as varchar) is null or al.severidad = :severidad)
            order by case al.severidad when 'CRITICA' then 0 when 'ADVERTENCIA' then 1 else 2 end,
                     al.fecha_generacion desc, al.id_alerta desc
            """;
    private static final String POR_ID = DESDE + " where al.id_alerta = :id";
    private static final String INSERTAR = """
            insert into alertas (tipo, severidad, id_activo, id_evento, clave, detalle, fecha_generacion, estado)
            values (:tipo, :severidad, :idActivo, :idEvento, :clave, :detalle, :fechaGeneracion, :estado)
            returning id_alerta
            """;
    private static final String ACTUALIZAR = """
            update alertas set detalle = :detalle, estado = :estado, atendida_por = :atendidaPor,
                               fecha_atencion = :fechaAtencion, comentario_atencion = :comentario,
                               fecha_resolucion = :fechaResolucion
            where id_alerta = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioAlertasJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Alerta> abiertas() {
        return jdbc.query(ABIERTAS, new MapSqlParameterSource(), RepositorioAlertasJdbc::alerta);
    }

    @Override
    public List<Alerta> buscar(FiltroAlertas filtro) {
        return jdbc.query(BUSCAR, new MapSqlParameterSource()
                .addValue("estado", filtro.estado() == null ? null : filtro.estado().name())
                .addValue("tipo", filtro.tipo() == null ? null : filtro.tipo().name())
                .addValue("severidad", filtro.severidad() == null ? null : filtro.severidad().name()),
                RepositorioAlertasJdbc::alerta);
    }

    @Override
    public Optional<Alerta> buscarPorId(Long id) {
        return jdbc.query(POR_ID, new MapSqlParameterSource("id", id), RepositorioAlertasJdbc::alerta).stream()
                .findFirst();
    }

    @Override
    public Alerta guardar(Alerta alerta) {
        if (alerta.getId() == null) {
            Long id = jdbc.queryForObject(INSERTAR, new MapSqlParameterSource()
                    .addValue("tipo", alerta.getTipo().name())
                    .addValue("severidad", alerta.getSeveridad().name())
                    .addValue("idActivo", alerta.getIdActivo())
                    .addValue("idEvento", alerta.getIdEvento())
                    .addValue("clave", alerta.getClave())
                    .addValue("detalle", alerta.getDetalle())
                    .addValue("fechaGeneracion", alerta.getFechaGeneracion())
                    .addValue("estado", alerta.getEstado().name()), Long.class);
            return obtener(id);
        }
        jdbc.update(ACTUALIZAR, new MapSqlParameterSource()
                .addValue("id", alerta.getId())
                .addValue("detalle", alerta.getDetalle())
                .addValue("estado", alerta.getEstado().name())
                .addValue("atendidaPor", alerta.getAtendidaPor())
                .addValue("fechaAtencion", alerta.getFechaAtencion())
                .addValue("comentario", alerta.getComentarioAtencion())
                .addValue("fechaResolucion", alerta.getFechaResolucion()));
        return obtener(alerta.getId());
    }

    private static Alerta alerta(ResultSet fila, int numero) throws SQLException {
        return Alerta.builder()
                .id(fila.getLong("id_alerta"))
                .tipo(TipoAlerta.valueOf(fila.getString("tipo")))
                .severidad(Severidad.valueOf(fila.getString("severidad")))
                .idActivo(entero(fila, "id_activo"))
                .idEvento(entero(fila, "id_evento"))
                .clave(fila.getString("clave"))
                .codigo(fila.getString("codigo_inventario"))
                .modelo(fila.getString("modelo"))
                .categoria(fila.getString("categoria"))
                .detalle(fila.getString("detalle"))
                .fechaGeneracion(fila.getObject("fecha_generacion", LocalDateTime.class))
                .estado(EstadoAlerta.valueOf(fila.getString("estado")))
                .atendidaPor(entero(fila, "atendida_por"))
                .nombreAtendio(fila.getString("nombre_atendio"))
                .fechaAtencion(fila.getObject("fecha_atencion", LocalDateTime.class))
                .comentarioAtencion(fila.getString("comentario_atencion"))
                .fechaResolucion(fila.getObject("fecha_resolucion", LocalDateTime.class))
                .build();
    }

    private static Integer entero(ResultSet fila, String columna) throws SQLException {
        int valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }
}
