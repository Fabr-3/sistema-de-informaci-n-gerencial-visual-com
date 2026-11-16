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
import com.visual.backend.subcontratacion.dominio.modelo.EstadoSubcontratacion;
import com.visual.backend.subcontratacion.dominio.modelo.Filtros;
import com.visual.backend.subcontratacion.dominio.modelo.Subcontratacion;
import com.visual.backend.subcontratacion.dominio.puerto.RepositorioSubcontrataciones;

/** Subcontrataciones con SQL: se leen junto con los nombres del proveedor, el proyecto y el evento. */
@Repository
public class RepositorioSubcontratacionesJdbc implements RepositorioSubcontrataciones {

    private static final String DESDE = """
            from subcontrataciones s
            join proveedores pr on pr.id_proveedor = s.id_proveedor
            join proyectos p on p.id_proyecto = s.id_proyecto
            left join eventos e on e.id_evento = s.id_evento
            """;
    private static final String SELECCION = """
            select s.*, pr.razon_social, p.nombre_proyecto, e.nombre_evento
            """ + DESDE;
    private static final String CONDICIONES = """
            where (cast(:idProyecto as integer) is null or s.id_proyecto = :idProyecto)
              and (cast(:idEvento as integer) is null or s.id_evento = :idEvento)
              and (cast(:idProveedor as integer) is null or s.id_proveedor = :idProveedor)
              and (cast(:desde as date) is null or s.fecha >= :desde)
              and (cast(:hasta as date) is null or s.fecha <= :hasta)
            """;
    private static final String BUSCAR = SELECCION + CONDICIONES + " order by s.fecha desc, s.id_subcontratacion desc";
    private static final String CONTAR = "select count(*) " + DESDE + CONDICIONES;
    private static final String POR_ID = SELECCION + " where s.id_subcontratacion = :id";
    private static final String INSERTAR = """
            insert into subcontrataciones (id_proveedor, id_servicio_prov, servicio, id_proyecto, id_evento,
                                           descripcion, costo, fecha, estado, id_movimiento, creado_por,
                                           fecha_creacion)
            values (:idProveedor, :idServicio, :servicio, :idProyecto, :idEvento, :descripcion, :costo, :fecha,
                    :estado, :idMovimiento, :creadoPor, :fechaCreacion)
            returning id_subcontratacion
            """;
    /** Solo cambia el estado: la subcontratacion no se edita. */
    private static final String ANULAR = """
            update subcontrataciones set estado = :estado, motivo_anulacion = :motivo, modificado_por = :modificadoPor,
                                         fecha_modificacion = :fechaModificacion
            where id_subcontratacion = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioSubcontratacionesJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<Subcontratacion> buscar(Filtros.Subcontrataciones filtro, SolicitudPagina solicitud) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("idProyecto", filtro.idProyecto())
                .addValue("idEvento", filtro.idEvento())
                .addValue("idProveedor", filtro.idProveedor())
                .addValue("desde", filtro.desde())
                .addValue("hasta", filtro.hasta());
        return PaginacionJdbc.paginar(jdbc, BUSCAR, CONTAR, parametros, solicitud,
                RepositorioSubcontratacionesJdbc::subcontratacion);
    }

    @Override
    public Optional<Subcontratacion> buscarPorId(Integer id) {
        List<Subcontratacion> encontradas = jdbc.query(POR_ID, new MapSqlParameterSource("id", id),
                RepositorioSubcontratacionesJdbc::subcontratacion);
        return encontradas.stream().findFirst();
    }

    @Override
    public Subcontratacion guardar(Subcontratacion subcontratacion) {
        if (subcontratacion.getId() != null) {
            jdbc.update(ANULAR, new MapSqlParameterSource()
                    .addValue("id", subcontratacion.getId())
                    .addValue("estado", subcontratacion.getEstado().name())
                    .addValue("motivo", subcontratacion.getMotivoAnulacion())
                    .addValue("modificadoPor", subcontratacion.getModificadoPor())
                    .addValue("fechaModificacion", subcontratacion.getFechaModificacion()));
            return obtener(subcontratacion.getId());
        }
        ElementoCatalogo servicio = subcontratacion.getServicio();
        ElementoCatalogo evento = subcontratacion.getEvento();
        Integer id = jdbc.queryForObject(INSERTAR, new MapSqlParameterSource()
                .addValue("idProveedor", subcontratacion.getProveedor().id())
                .addValue("idServicio", servicio == null ? null : servicio.id())
                .addValue("servicio", servicio == null ? null : servicio.nombre())
                .addValue("idProyecto", subcontratacion.getProyecto().id())
                .addValue("idEvento", evento == null ? null : evento.id())
                .addValue("descripcion", subcontratacion.getDescripcion())
                .addValue("costo", subcontratacion.getCosto())
                .addValue("fecha", subcontratacion.getFecha())
                .addValue("estado", subcontratacion.getEstado().name())
                .addValue("idMovimiento", subcontratacion.getIdMovimiento())
                .addValue("creadoPor", subcontratacion.getCreadoPor())
                .addValue("fechaCreacion", subcontratacion.getFechaCreacion()), Integer.class);
        return obtener(id);
    }

    private static Subcontratacion subcontratacion(ResultSet fila, int numero) throws SQLException {
        String servicio = fila.getString("servicio");
        Integer idEvento = entero(fila, "id_evento");
        return Subcontratacion.builder()
                .id(fila.getInt("id_subcontratacion"))
                .proveedor(new ElementoCatalogo(fila.getInt("id_proveedor"), fila.getString("razon_social")))
                .servicio(servicio == null ? null : new ElementoCatalogo(entero(fila, "id_servicio_prov"), servicio))
                .proyecto(new ElementoCatalogo(fila.getInt("id_proyecto"), fila.getString("nombre_proyecto")))
                .evento(idEvento == null ? null : new ElementoCatalogo(idEvento, fila.getString("nombre_evento")))
                .descripcion(fila.getString("descripcion"))
                .costo(fila.getBigDecimal("costo"))
                .fecha(fila.getObject("fecha", LocalDate.class))
                .estado(EstadoSubcontratacion.valueOf(fila.getString("estado")))
                .motivoAnulacion(fila.getString("motivo_anulacion"))
                .idMovimiento(entero(fila, "id_movimiento"))
                .creadoPor(entero(fila, "creado_por"))
                .fechaCreacion(fila.getObject("fecha_creacion", LocalDateTime.class))
                .modificadoPor(entero(fila, "modificado_por"))
                .fechaModificacion(fila.getObject("fecha_modificacion", LocalDateTime.class))
                .build();
    }
}
