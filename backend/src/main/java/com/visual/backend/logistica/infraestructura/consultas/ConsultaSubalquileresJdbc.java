package com.visual.backend.logistica.infraestructura.consultas;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.logistica.dominio.modelo.EquipoSubalquilado;
import com.visual.backend.logistica.dominio.puerto.ConsultaSubalquileres;

/** Lee las subcontrataciones con modelo (subalquileres) con SQL de solo lectura. */
@Repository
public class ConsultaSubalquileresJdbc implements ConsultaSubalquileres {

    private static final String DEL_EVENTO = """
            select s.id_subcontratacion, f.nombre || ' ' || mo.nombre as modelo, s.cantidad, pr.razon_social, s.costo
            from subcontrataciones s
            join modelos_activo mo on mo.id_modelo = s.id_modelo
            join fabricantes f on f.id_fabricante = mo.id_fabricante
            join proveedores pr on pr.id_proveedor = s.id_proveedor
            where s.id_evento = :idEvento and s.estado = 'REGISTRADA'
            order by modelo, s.id_subcontratacion
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaSubalquileresJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<EquipoSubalquilado> delEvento(Integer idEvento) {
        return jdbc.query(DEL_EVENTO, new MapSqlParameterSource("idEvento", idEvento), (fila, numero) ->
                new EquipoSubalquilado(fila.getInt("id_subcontratacion"), fila.getString("modelo"),
                        fila.getInt("cantidad"), fila.getString("razon_social"), fila.getBigDecimal("costo")));
    }
}
