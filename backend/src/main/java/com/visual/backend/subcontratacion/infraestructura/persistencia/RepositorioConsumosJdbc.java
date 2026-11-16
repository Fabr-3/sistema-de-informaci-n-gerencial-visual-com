package com.visual.backend.subcontratacion.infraestructura.persistencia;

import static com.visual.backend.subcontratacion.infraestructura.persistencia.PaginacionJdbc.entero;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;
import com.visual.backend.subcontratacion.dominio.modelo.ConsumoProyecto;
import com.visual.backend.subcontratacion.dominio.modelo.EstadoConsumo;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioConsumos;

/** Consumos con SQL, junto con los nombres del consumible, el proyecto, el evento y quien lo registro. */
@Repository
public class RepositorioConsumosJdbc implements RepositorioConsumos {

    /** Valor imposible de id: deja vacio el listado de un tecnico sin eventos asignados. */
    private static final int SIN_EVENTOS = -1;
    private static final String DESDE = """
            from consumos_proyecto cp
            join consumibles c on c.id_consumible = cp.id_consumible
            join proyectos p on p.id_proyecto = cp.id_proyecto
            left join eventos e on e.id_evento = cp.id_evento
            left join usuarios u on u.id_usuario = cp.registrado_por
            """;
    private static final String SELECCION = """
            select cp.*, c.nombre, c.unidad_medida, p.nombre_proyecto, e.nombre_evento,
                   trim(u.nombres || ' ' || u.apellido_paterno) as nombre_registrador
            """ + DESDE;
    private static final String CONDICIONES = """
            where (cast(:idProyecto as integer) is null or cp.id_proyecto = :idProyecto)
              and (cast(:idEvento as integer) is null or cp.id_evento = :idEvento)
              and (cast(:idConsumible as integer) is null or cp.id_consumible = :idConsumible)
              and (cast(:desde as date) is null or cp.fecha >= :desde)
              and (cast(:hasta as date) is null or cp.fecha <= :hasta)
              and (not :limitado or cp.id_evento in (:idsEventos))
            """;
    private static final String BUSCAR = SELECCION + CONDICIONES + " order by cp.fecha desc, cp.id_consumo desc";
    private static final String CONTAR = "select count(*) " + DESDE + CONDICIONES;
    private static final String POR_ID = SELECCION + " where cp.id_consumo = :id";
    private static final String INSERTAR = """
            insert into consumos_proyecto (id_consumible, id_proyecto, id_evento, cantidad, costo_unitario, fecha,
                                           estado, registrado_por, fecha_creacion)
            values (:idConsumible, :idProyecto, :idEvento, :cantidad, :costoUnitario, :fecha, :estado,
                    :registradoPor, :fechaCreacion)
            returning id_consumo
            """;
    /** Solo cambia el estado: el consumo no se edita. */
    private static final String ANULAR = """
            update consumos_proyecto set estado = :estado, motivo_anulacion = :motivo, modificado_por = :modificadoPor,
                                         fecha_modificacion = :fechaModificacion
            where id_consumo = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioConsumosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<ConsumoProyecto> buscar(Filtros.Consumos filtro, SolicitudPagina solicitud) {
        List<Integer> eventos = filtro.idsEventos();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("idProyecto", filtro.idProyecto())
                .addValue("idEvento", filtro.idEvento())
                .addValue("idConsumible", filtro.idConsumible())
                .addValue("desde", filtro.desde())
                .addValue("hasta", filtro.hasta())
                .addValue("limitado", eventos != null)
                .addValue("idsEventos", eventos == null || eventos.isEmpty() ? List.of(SIN_EVENTOS) : eventos);
        return PaginacionJdbc.paginar(jdbc, BUSCAR, CONTAR, parametros, solicitud, RepositorioConsumosJdbc::consumo);
    }

    @Override
    public Optional<ConsumoProyecto> buscarPorId(Integer id) {
        return jdbc.query(POR_ID, new MapSqlParameterSource("id", id), RepositorioConsumosJdbc::consumo).stream()
                .findFirst();
    }

    @Override
    public ConsumoProyecto guardar(ConsumoProyecto consumo) {
        if (consumo.getId() != null) {
            jdbc.update(ANULAR, new MapSqlParameterSource()
                    .addValue("id", consumo.getId())
                    .addValue("estado", consumo.getEstado().name())
                    .addValue("motivo", consumo.getMotivoAnulacion())
                    .addValue("modificadoPor", consumo.getModificadoPor())
                    .addValue("fechaModificacion", consumo.getFechaModificacion()));
            return obtener(consumo.getId());
        }
        ElementoCatalogo evento = consumo.getEvento();
        Integer id = jdbc.queryForObject(INSERTAR, new MapSqlParameterSource()
                .addValue("idConsumible", consumo.getConsumible().id())
                .addValue("idProyecto", consumo.getProyecto().id())
                .addValue("idEvento", evento == null ? null : evento.id())
                .addValue("cantidad", consumo.getCantidad())
                .addValue("costoUnitario", consumo.getCostoUnitario())
                .addValue("fecha", consumo.getFecha())
                .addValue("estado", consumo.getEstado().name())
                .addValue("registradoPor", consumo.getRegistradoPor())
                .addValue("fechaCreacion", consumo.getFechaCreacion()), Integer.class);
        return obtener(id);
    }

    private static ConsumoProyecto consumo(ResultSet fila, int numero) throws SQLException {
        Integer idEvento = entero(fila, "id_evento");
        return ConsumoProyecto.builder()
                .id(fila.getInt("id_consumo"))
                .consumible(new ElementoCatalogo(fila.getInt("id_consumible"), fila.getString("nombre")))
                .unidadMedida(fila.getString("unidad_medida"))
                .proyecto(new ElementoCatalogo(fila.getInt("id_proyecto"), fila.getString("nombre_proyecto")))
                .evento(idEvento == null ? null : new ElementoCatalogo(idEvento, fila.getString("nombre_evento")))
                .cantidad(fila.getBigDecimal("cantidad"))
                .costoUnitario(fila.getBigDecimal("costo_unitario"))
                .fecha(fila.getObject("fecha", LocalDate.class))
                .estado(EstadoConsumo.valueOf(fila.getString("estado")))
                .motivoAnulacion(fila.getString("motivo_anulacion"))
                .registradoPor(entero(fila, "registrado_por"))
                .nombreRegistrador(fila.getString("nombre_registrador"))
                .fechaCreacion(fila.getObject("fecha_creacion", LocalDateTime.class))
                .modificadoPor(entero(fila, "modificado_por"))
                .fechaModificacion(fila.getObject("fecha_modificacion", LocalDateTime.class))
                .build();
    }
}
