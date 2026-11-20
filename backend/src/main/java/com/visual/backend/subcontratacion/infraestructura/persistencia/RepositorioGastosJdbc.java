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
import com.visual.backend.subcontratacion.dominio.modelo.Comprobante;
import com.visual.backend.subcontratacion.dominio.modelo.EstadoConsumo;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.GastoEvento;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioGastos;

/** Gastos del evento con SQL, junto con los nombres del proyecto, el evento y quien lo registro. */
@Repository
public class RepositorioGastosJdbc implements RepositorioGastos {

    /** Valor imposible de id: deja vacio el listado de un tecnico sin eventos asignados. */
    private static final int SIN_EVENTOS = -1;
    private static final String DESDE = """
            from gastos_evento g
            join proyectos p on p.id_proyecto = g.id_proyecto
            join eventos e on e.id_evento = g.id_evento
            left join usuarios u on u.id_usuario = g.registrado_por
            """;
    private static final String SELECCION = """
            select g.*, p.nombre_proyecto, e.nombre_evento,
                   trim(u.nombres || ' ' || u.apellido_paterno) as nombre_registrador
            """ + DESDE;
    private static final String CONDICIONES = """
            where (cast(:idProyecto as integer) is null or g.id_proyecto = :idProyecto)
              and (cast(:idEvento as integer) is null or g.id_evento = :idEvento)
              and (cast(:desde as date) is null or g.fecha >= :desde)
              and (cast(:hasta as date) is null or g.fecha <= :hasta)
              and (not :limitado or g.id_evento in (:idsEventos))
            """;
    private static final String BUSCAR = SELECCION + CONDICIONES + " order by g.fecha desc, g.id_gasto desc";
    private static final String CONTAR = "select count(*) " + DESDE + CONDICIONES;
    private static final String POR_ID = SELECCION + " where g.id_gasto = :id";
    private static final String INSERTAR = """
            insert into gastos_evento (id_proyecto, id_evento, descripcion, monto, fecha, comprobante_archivo,
                                       comprobante_nombre, comprobante_tipo, estado, id_movimiento,
                                       registrado_por, fecha_creacion)
            values (:idProyecto, :idEvento, :descripcion, :monto, :fecha, :archivo, :nombreArchivo, :tipoArchivo,
                    :estado, :idMovimiento, :registradoPor, :fechaCreacion)
            returning id_gasto
            """;
    /** Solo cambia el estado: el gasto no se edita. */
    private static final String ANULAR = """
            update gastos_evento set estado = :estado, motivo_anulacion = :motivo, modificado_por = :modificadoPor,
                                     fecha_modificacion = :fechaModificacion
            where id_gasto = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioGastosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<GastoEvento> buscar(Filtros.Gastos filtro, SolicitudPagina solicitud) {
        List<Integer> eventos = filtro.idsEventos();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("idProyecto", filtro.idProyecto())
                .addValue("idEvento", filtro.idEvento())
                .addValue("desde", filtro.desde())
                .addValue("hasta", filtro.hasta())
                .addValue("limitado", eventos != null)
                .addValue("idsEventos", eventos == null || eventos.isEmpty() ? List.of(SIN_EVENTOS) : eventos);
        return PaginacionJdbc.paginar(jdbc, BUSCAR, CONTAR, parametros, solicitud, RepositorioGastosJdbc::gasto);
    }

    @Override
    public Optional<GastoEvento> buscarPorId(Integer id) {
        return jdbc.query(POR_ID, new MapSqlParameterSource("id", id), RepositorioGastosJdbc::gasto).stream()
                .findFirst();
    }

    @Override
    public GastoEvento guardar(GastoEvento gasto) {
        if (gasto.getId() != null) {
            jdbc.update(ANULAR, new MapSqlParameterSource()
                    .addValue("id", gasto.getId())
                    .addValue("estado", gasto.getEstado().name())
                    .addValue("motivo", gasto.getMotivoAnulacion())
                    .addValue("modificadoPor", gasto.getModificadoPor())
                    .addValue("fechaModificacion", gasto.getFechaModificacion()));
            return obtener(gasto.getId());
        }
        Comprobante comprobante = gasto.getComprobante();
        Integer id = jdbc.queryForObject(INSERTAR, new MapSqlParameterSource()
                .addValue("idProyecto", gasto.getProyecto().id())
                .addValue("idEvento", gasto.getEvento().id())
                .addValue("descripcion", gasto.getDescripcion())
                .addValue("monto", gasto.getMonto())
                .addValue("fecha", gasto.getFecha())
                .addValue("archivo", comprobante.archivo())
                .addValue("nombreArchivo", comprobante.nombreOriginal())
                .addValue("tipoArchivo", comprobante.tipoContenido())
                .addValue("estado", gasto.getEstado().name())
                .addValue("idMovimiento", gasto.getIdMovimiento())
                .addValue("registradoPor", gasto.getRegistradoPor())
                .addValue("fechaCreacion", gasto.getFechaCreacion()), Integer.class);
        return obtener(id);
    }

    private static GastoEvento gasto(ResultSet fila, int numero) throws SQLException {
        return GastoEvento.builder()
                .id(fila.getInt("id_gasto"))
                .proyecto(new ElementoCatalogo(fila.getInt("id_proyecto"), fila.getString("nombre_proyecto")))
                .evento(new ElementoCatalogo(fila.getInt("id_evento"), fila.getString("nombre_evento")))
                .descripcion(fila.getString("descripcion"))
                .monto(fila.getBigDecimal("monto"))
                .fecha(fila.getObject("fecha", LocalDate.class))
                .comprobante(new Comprobante(fila.getString("comprobante_archivo"),
                        fila.getString("comprobante_nombre"), fila.getString("comprobante_tipo")))
                .estado(EstadoConsumo.valueOf(fila.getString("estado")))
                .motivoAnulacion(fila.getString("motivo_anulacion"))
                .idMovimiento(entero(fila, "id_movimiento"))
                .registradoPor(entero(fila, "registrado_por"))
                .nombreRegistrador(fila.getString("nombre_registrador"))
                .fechaCreacion(fila.getObject("fecha_creacion", LocalDateTime.class))
                .modificadoPor(entero(fila, "modificado_por"))
                .fechaModificacion(fila.getObject("fecha_modificacion", LocalDateTime.class))
                .build();
    }
}
