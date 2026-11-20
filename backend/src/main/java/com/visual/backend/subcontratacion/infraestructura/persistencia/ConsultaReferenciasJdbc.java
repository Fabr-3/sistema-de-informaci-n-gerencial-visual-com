package com.visual.backend.subcontratacion.infraestructura.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.subcontratacion.dominio.modelo.ProveedorReferencia;
import com.visual.backend.subcontratacion.dominio.modelo.ProyectoReferencia;
import com.visual.backend.subcontratacion.dominio.puerto.ConsultaReferencias;

/** Lectura con SQL de las tablas de comercial y finanzas, sin usar sus entidades. */
@Repository
public class ConsultaReferenciasJdbc implements ConsultaReferencias {

    private static final String PROYECTO = """
            select id_proyecto, nombre_proyecto, estado in ('PLANIFICACION', 'EN_EJECUCION') as vigente
            from proyectos where id_proyecto = :id and fecha_eliminacion is null
            """;
    private static final String EVENTO = """
            select id_evento, nombre_evento from eventos
            where id_evento = :id and id_proyecto = :idProyecto and fecha_eliminacion is null
              and estado <> 'CANCELADO'
            """;
    private static final String PROVEEDOR = """
            select id_proveedor, razon_social, fecha_eliminacion is null and activo as vigente
            from proveedores where id_proveedor = :id
            """;
    private static final String SERVICIOS = """
            select id_servicio_prov, descripcion_servicio from servicios_proveedor
            where id_proveedor = :idProveedor order by descripcion_servicio
            """;
    private static final String SERVICIO = """
            select id_servicio_prov, descripcion_servicio from servicios_proveedor
            where id_servicio_prov = :id and id_proveedor = :idProveedor
            """;

    private static final String MODELO = """
            select mo.id_modelo, f.nombre || ' ' || mo.nombre as modelo from modelos_activo mo
            join fabricantes f on f.id_fabricante = mo.id_fabricante
            where mo.id_modelo = :id and mo.tipo_control = 'UNIDAD'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReferenciasJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProyectoReferencia> proyecto(Integer idProyecto) {
        return jdbc.query(PROYECTO, new MapSqlParameterSource("id", idProyecto), (fila, numero) ->
                new ProyectoReferencia(fila.getInt("id_proyecto"), fila.getString("nombre_proyecto"),
                        fila.getBoolean("vigente"))).stream().findFirst();
    }

    @Override
    public Optional<ElementoCatalogo> eventoDelProyecto(Integer idEvento, Integer idProyecto) {
        return jdbc.query(EVENTO, new MapSqlParameterSource().addValue("id", idEvento)
                .addValue("idProyecto", idProyecto), (fila, numero) -> new ElementoCatalogo(fila.getInt("id_evento"),
                fila.getString("nombre_evento"))).stream().findFirst();
    }

    @Override
    public Optional<ProveedorReferencia> proveedor(Integer idProveedor) {
        return jdbc.query(PROVEEDOR, new MapSqlParameterSource("id", idProveedor), (fila, numero) ->
                new ProveedorReferencia(fila.getInt("id_proveedor"), fila.getString("razon_social"),
                        fila.getBoolean("vigente"))).stream().findFirst();
    }

    @Override
    public List<ElementoCatalogo> serviciosDelProveedor(Integer idProveedor) {
        return jdbc.query(SERVICIOS, new MapSqlParameterSource("idProveedor", idProveedor), (fila, numero) ->
                new ElementoCatalogo(fila.getInt("id_servicio_prov"), fila.getString("descripcion_servicio")));
    }

    @Override
    public Optional<ElementoCatalogo> servicioDelProveedor(Integer idServicio, Integer idProveedor) {
        return jdbc.query(SERVICIO, new MapSqlParameterSource().addValue("id", idServicio)
                .addValue("idProveedor", idProveedor), (fila, numero) -> new ElementoCatalogo(
                fila.getInt("id_servicio_prov"), fila.getString("descripcion_servicio"))).stream().findFirst();
    }

    @Override
    public Optional<ElementoCatalogo> modeloPorUnidad(Integer idModelo) {
        return jdbc.query(MODELO, new MapSqlParameterSource("id", idModelo), (fila, numero) ->
                new ElementoCatalogo(fila.getInt("id_modelo"), fila.getString("modelo"))).stream().findFirst();
    }
}
