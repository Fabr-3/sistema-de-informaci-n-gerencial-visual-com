package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.visual.backend.logistica.dominio.modelo.ConflictoReserva;
import com.visual.backend.logistica.dominio.modelo.EstadoReserva;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;
import com.visual.backend.logistica.dominio.modelo.ReservaActivo;
import com.visual.backend.logistica.dominio.puerto.RepositorioReservas;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.ReservaActivoEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.ReservaActivoJpaRepositorio;

/**
 * Reservas con JPA; los conflictos se leen con SQL para obtener el codigo del
 * activo y el evento sin usar entidades de otros modulos.
 */
@Repository
public class RepositorioReservasJpa implements RepositorioReservas {

    private static final String SUPERPUESTAS = """
            select r.id_activo, a.codigo_inventario, e.id_evento, e.nombre_evento, r.fecha_salida, r.fecha_retorno
            from reservas_activo r
            join activos a on a.id_activo = r.id_activo
            join planes_logisticos p on p.id_plan = r.id_plan
            join eventos e on e.id_evento = p.id_evento
            where r.id_activo = :idActivo and r.estado = 'ACTIVA'
              and r.fecha_salida < :retorno and r.fecha_retorno > :salida
              and (cast(:idExcluida as integer) is null or r.id_reserva <> :idExcluida)
            order by r.fecha_salida
            """;

    private final ReservaActivoJpaRepositorio reservas;
    private final NamedParameterJdbcTemplate jdbc;

    public RepositorioReservasJpa(ReservaActivoJpaRepositorio reservas, NamedParameterJdbcTemplate jdbc) {
        this.reservas = reservas;
        this.jdbc = jdbc;
    }

    @Override
    public List<ReservaActivo> buscarActivasPorPlan(Integer idPlan) {
        return reservas.findByIdPlanAndEstadoOrderByIdAsc(idPlan, EstadoReserva.ACTIVA).stream()
                .map(RepositorioReservasJpa::aDominio).toList();
    }

    @Override
    public Optional<ReservaActivo> buscarPorId(Integer idReserva) {
        return reservas.findById(idReserva).map(RepositorioReservasJpa::aDominio);
    }

    /** Se sincroniza antes de consultar para ver los cambios pendientes de la misma transaccion. */
    @Override
    public List<ConflictoReserva> buscarSuperpuestas(Integer idActivo, PeriodoReserva periodo,
                                                     Integer idReservaExcluida) {
        reservas.flush();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("idActivo", idActivo)
                .addValue("salida", periodo.salida())
                .addValue("retorno", periodo.retorno())
                .addValue("idExcluida", idReservaExcluida);
        return jdbc.query(SUPERPUESTAS, parametros, RepositorioReservasJpa::conflicto);
    }

    @Override
    public ReservaActivo guardar(ReservaActivo reserva) {
        return aDominio(reservas.saveAndFlush(aEntidad(reserva)));
    }

    @Override
    public List<ReservaActivo> guardarTodas(List<ReservaActivo> lista) {
        List<ReservaActivoEntidad> guardadas = reservas.saveAllAndFlush(
                lista.stream().map(this::aEntidad).toList());
        return guardadas.stream().map(RepositorioReservasJpa::aDominio).toList();
    }

    private ReservaActivoEntidad aEntidad(ReservaActivo reserva) {
        ReservaActivoEntidad entidad = reserva.getId() == null ? new ReservaActivoEntidad()
                : reservas.getReferenceById(reserva.getId());
        if (reserva.getId() == null) {
            entidad.setIdPlan(reserva.getIdPlan());
            entidad.setIdActivo(reserva.getIdActivo());
            entidad.setCreadoPor(reserva.getCreadoPor());
            entidad.setFechaCreacion(reserva.getFechaCreacion());
        }
        entidad.setFechaSalida(reserva.getPeriodo().salida());
        entidad.setFechaRetorno(reserva.getPeriodo().retorno());
        entidad.setEstado(reserva.getEstado());
        entidad.setFechaLiberacion(reserva.getFechaLiberacion());
        entidad.setMotivoLiberacion(reserva.getMotivoLiberacion());
        entidad.setModificadoPor(reserva.getModificadoPor());
        entidad.setFechaModificacion(reserva.getFechaModificacion());
        return entidad;
    }

    private static ReservaActivo aDominio(ReservaActivoEntidad entidad) {
        return ReservaActivo.builder()
                .id(entidad.getId())
                .idPlan(entidad.getIdPlan())
                .idActivo(entidad.getIdActivo())
                .periodo(new PeriodoReserva(entidad.getFechaSalida(), entidad.getFechaRetorno()))
                .estado(entidad.getEstado())
                .fechaLiberacion(entidad.getFechaLiberacion())
                .motivoLiberacion(entidad.getMotivoLiberacion())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    private static ConflictoReserva conflicto(ResultSet fila, int numero) throws SQLException {
        return new ConflictoReserva(fila.getInt("id_activo"), fila.getString("codigo_inventario"),
                fila.getInt("id_evento"), fila.getString("nombre_evento"),
                fila.getObject("fecha_salida", LocalDateTime.class),
                fila.getObject("fecha_retorno", LocalDateTime.class));
    }
}
