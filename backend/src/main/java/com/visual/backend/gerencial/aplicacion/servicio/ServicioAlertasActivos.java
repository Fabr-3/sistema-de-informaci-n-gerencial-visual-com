package com.visual.backend.gerencial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.gerencial.dominio.modelo.Alerta;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.EstadoAlerta;
import com.visual.backend.gerencial.dominio.modelo.FiltroAlertas;
import com.visual.backend.gerencial.dominio.modelo.ResumenAlertas;
import com.visual.backend.gerencial.dominio.puerto.ConsultaAlertasActivos;
import com.visual.backend.gerencial.dominio.puerto.RepositorioAlertas;

/**
 * Alertas del estado de los activos, sus reservas y los consumibles. Se
 * calculan sobre los datos y se sincronizan con las registradas: una nueva
 * nace pendiente, una atendida no se reabre y la que ya no se presenta pasa
 * a resuelta.
 */
@Service
public class ServicioAlertasActivos {

    public static final String ENTIDAD = "alertas";

    private final ConsultaAlertasActivos consulta;
    private final RepositorioAlertas repositorio;
    private final ParametrosGerencial parametros;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioAlertasActivos(ConsultaAlertasActivos consulta, RepositorioAlertas repositorio,
                                  ParametrosGerencial parametros, ServicioAuditoria auditoria,
                                  ContextoSolicitud contexto, Clock reloj) {
        this.consulta = consulta;
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    /** Situaciones actuales, de la mas a la menos urgente. */
    @Transactional(readOnly = true)
    public List<AlertaActivo> calcular() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        return Stream.of(consulta.reservasEnRiesgo(ahora),
                        consulta.mantenimientoPreventivo(),
                        consulta.mantenimientosProlongados(hoy.minusDays(parametros.diasMantenimientoProlongado()), hoy),
                        consulta.modulosDefectuosos(),
                        consulta.fallasRecurrentes(parametros.correctivosFallasRecurrentes(), hoy.minusYears(1)),
                        consulta.sinUso(hoy.minusDays(parametros.diasSinUso()), hoy),
                        consulta.stockBajo())
                .flatMap(List::stream)
                .sorted(AlertaActivo.POR_URGENCIA)
                .toList();
    }

    @Transactional
    public void sincronizar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Map<String, AlertaActivo> calculadas = calcular().stream().collect(Collectors.toMap(
                alerta -> Alerta.identidad(alerta.tipo(), alerta.clave()), Function.identity(),
                (primera, repetida) -> primera, LinkedHashMap::new));
        for (Alerta abierta : repositorio.abiertas()) {
            AlertaActivo vigente = calculadas.remove(abierta.identidad());
            if (vigente == null) {
                abierta.resolver(ahora);
                repositorio.guardar(abierta);
            } else if (abierta.actualizar(vigente)) {
                repositorio.guardar(abierta);
            }
        }
        calculadas.values().forEach(nueva -> repositorio.guardar(Alerta.generar(nueva, ahora)));
    }

    /** Sincroniza antes de listar; sin estado en el filtro se listan las pendientes. */
    @Transactional
    public List<Alerta> listar(FiltroAlertas filtro) {
        sincronizar();
        return repositorio.buscar(filtro);
    }

    /** Alertas pendientes con el formato de las calculadas (reportes). */
    @Transactional
    public List<AlertaActivo> pendientes() {
        return listar(new FiltroAlertas(EstadoAlerta.PENDIENTE, null, null)).stream()
                .map(Alerta::comoCalculada).sorted(AlertaActivo.POR_URGENCIA).toList();
    }

    @Transactional
    public ResumenAlertas resumenPendientes() {
        return ResumenAlertas.de(pendientes());
    }

    @Transactional
    public Alerta atender(Long id, String comentario) {
        Alerta alerta = repositorio.obtener(id);
        EstadoAlerta anterior = alerta.getEstado();
        alerta.atender(comentario, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        Alerta atendida = repositorio.guardar(alerta);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id,
                Map.of("estado", anterior.name()),
                Map.of("estado", atendida.getEstado().name(), "comentario", atendida.getComentarioAtencion()),
                atendida.getTipo().name() + " · " + atendida.getDetalle());
        return atendida;
    }
}
