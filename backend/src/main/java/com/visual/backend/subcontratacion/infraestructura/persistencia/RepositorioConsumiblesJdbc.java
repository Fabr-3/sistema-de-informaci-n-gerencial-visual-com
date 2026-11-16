package com.visual.backend.subcontratacion.infraestructura.persistencia;

import static com.visual.backend.subcontratacion.infraestructura.persistencia.PaginacionJdbc.entero;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.subcontratacion.dominio.modelo.Consumible;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;
import com.visual.backend.subcontratacion.dominio.modelo.TipoMovimientoConsumible;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioConsumibles;

@Repository
public class RepositorioConsumiblesJdbc implements RepositorioConsumibles {

    private static final String CONDICIONES = """
            from consumibles c
            where (cast(:patron as text) is null or lower(c.nombre) like :patron)
              and (cast(:activo as boolean) is null or c.activo = :activo)
              and (not :soloStockBajo or (c.stock_minimo is not null and c.stock_actual <= c.stock_minimo))
            """;
    private static final String BUSCAR = "select c.* " + CONDICIONES + " order by c.nombre";
    private static final String CONTAR = "select count(*) " + CONDICIONES;
    private static final String POR_ID = "select c.* from consumibles c where c.id_consumible = :id";
    private static final String POR_ID_BLOQUEANDO = POR_ID + " for update";
    private static final String EXISTE_NOMBRE = """
            select exists (select 1 from consumibles where lower(nombre) = lower(:nombre)
                           and (cast(:idExcluido as integer) is null or id_consumible <> :idExcluido))
            """;
    private static final String INSERTAR = """
            insert into consumibles (nombre, unidad_medida, stock_actual, stock_minimo, costo_unitario, activo,
                                     creado_por, fecha_creacion)
            values (:nombre, :unidadMedida, :stockActual, :stockMinimo, :costoUnitario, :activo, :creadoPor,
                    :fechaCreacion)
            returning id_consumible
            """;
    private static final String ACTUALIZAR = """
            update consumibles set nombre = :nombre, unidad_medida = :unidadMedida, stock_actual = :stockActual,
                                   stock_minimo = :stockMinimo, costo_unitario = :costoUnitario, activo = :activo,
                                   modificado_por = :modificadoPor, fecha_modificacion = :fechaModificacion
            where id_consumible = :id
            """;
    private static final String INSERTAR_MOVIMIENTO = """
            insert into movimientos_consumible (id_consumible, tipo, cantidad, fecha, motivo, creado_por)
            values (:idConsumible, :tipo, :cantidad, :fecha, :motivo, :creadoPor)
            """;
    private static final String MOVIMIENTOS = """
            select m.*, trim(u.nombres || ' ' || u.apellido_paterno) as nombre_creador
            from movimientos_consumible m
            left join usuarios u on u.id_usuario = m.creado_por
            where m.id_consumible = :idConsumible
            order by m.fecha desc, m.id_movimiento desc
            """;
    private static final String CONTAR_MOVIMIENTOS =
            "select count(*) from movimientos_consumible where id_consumible = :idConsumible";

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioConsumiblesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<Consumible> buscar(Filtros.Consumibles filtro, SolicitudPagina solicitud) {
        String texto = filtro.texto();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("patron", texto == null || texto.isBlank() ? null : ConsultasJpa.patronContiene(texto))
                .addValue("activo", filtro.activo())
                .addValue("soloStockBajo", filtro.soloStockBajo());
        return PaginacionJdbc.paginar(jdbc, BUSCAR, CONTAR, parametros, solicitud,
                RepositorioConsumiblesJdbc::consumible);
    }

    @Override
    public Optional<Consumible> buscarPorId(Integer id) {
        return consulta(POR_ID, id);
    }

    @Override
    public Consumible obtenerParaActualizar(Integer id) {
        return consulta(POR_ID_BLOQUEANDO, id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el consumible", id));
    }

    @Override
    public boolean existeNombre(String nombre, Integer idExcluido) {
        return Boolean.TRUE.equals(jdbc.queryForObject(EXISTE_NOMBRE, new MapSqlParameterSource()
                .addValue("nombre", nombre).addValue("idExcluido", idExcluido), Boolean.class));
    }

    @Override
    public Consumible guardar(Consumible consumible) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("nombre", consumible.getNombre())
                .addValue("unidadMedida", consumible.getUnidadMedida())
                .addValue("stockActual", consumible.getStockActual())
                .addValue("stockMinimo", consumible.getStockMinimo())
                .addValue("costoUnitario", consumible.getCostoUnitario())
                .addValue("activo", consumible.isActivo());
        if (consumible.getId() == null) {
            Integer id = jdbc.queryForObject(INSERTAR, parametros.addValue("creadoPor", consumible.getCreadoPor())
                    .addValue("fechaCreacion", consumible.getFechaCreacion()), Integer.class);
            return obtener(id);
        }
        jdbc.update(ACTUALIZAR, parametros.addValue("id", consumible.getId())
                .addValue("modificadoPor", consumible.getModificadoPor())
                .addValue("fechaModificacion", consumible.getFechaModificacion()));
        return obtener(consumible.getId());
    }

    @Override
    public void registrarMovimiento(MovimientoConsumible movimiento) {
        jdbc.update(INSERTAR_MOVIMIENTO, new MapSqlParameterSource()
                .addValue("idConsumible", movimiento.idConsumible())
                .addValue("tipo", movimiento.tipo().name())
                .addValue("cantidad", movimiento.cantidad())
                .addValue("fecha", movimiento.fecha())
                .addValue("motivo", movimiento.motivo())
                .addValue("creadoPor", movimiento.creadoPor()));
    }

    @Override
    public Pagina<MovimientoConsumible> movimientos(Integer idConsumible, SolicitudPagina solicitud) {
        return PaginacionJdbc.paginar(jdbc, MOVIMIENTOS, CONTAR_MOVIMIENTOS,
                new MapSqlParameterSource("idConsumible", idConsumible), solicitud, (fila, numero) ->
                        new MovimientoConsumible(fila.getInt("id_movimiento"), fila.getInt("id_consumible"),
                                TipoMovimientoConsumible.valueOf(fila.getString("tipo")),
                                fila.getBigDecimal("cantidad"), fila.getObject("fecha", LocalDateTime.class),
                                fila.getString("motivo"), entero(fila, "creado_por"),
                                fila.getString("nombre_creador")));
    }

    private Optional<Consumible> consulta(String sql, Integer id) {
        List<Consumible> encontrados = jdbc.query(sql, new MapSqlParameterSource("id", id),
                RepositorioConsumiblesJdbc::consumible);
        return encontrados.stream().findFirst();
    }

    private static Consumible consumible(ResultSet fila, int numero) throws SQLException {
        return Consumible.builder()
                .id(fila.getInt("id_consumible"))
                .nombre(fila.getString("nombre"))
                .unidadMedida(fila.getString("unidad_medida"))
                .stockActual(fila.getBigDecimal("stock_actual"))
                .stockMinimo(fila.getBigDecimal("stock_minimo"))
                .costoUnitario(fila.getBigDecimal("costo_unitario"))
                .activo(fila.getBoolean("activo"))
                .creadoPor(entero(fila, "creado_por"))
                .fechaCreacion(fila.getObject("fecha_creacion", LocalDateTime.class))
                .modificadoPor(entero(fila, "modificado_por"))
                .fechaModificacion(fila.getObject("fecha_modificacion", LocalDateTime.class))
                .build();
    }
}
