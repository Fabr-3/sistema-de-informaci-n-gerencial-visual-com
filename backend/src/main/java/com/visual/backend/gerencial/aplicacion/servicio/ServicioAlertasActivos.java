package com.visual.backend.gerencial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.puerto.ConsultaAlertasActivos;

/** Alertas vigentes del estado de los activos, de la mas a la menos urgente. */
@Service
public class ServicioAlertasActivos {

    private final ConsultaAlertasActivos consulta;
    private final ParametrosGerencial parametros;
    private final Clock reloj;

    public ServicioAlertasActivos(ConsultaAlertasActivos consulta, ParametrosGerencial parametros, Clock reloj) {
        this.consulta = consulta;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<AlertaActivo> listar() {
        LocalDate hoy = LocalDate.now(reloj);
        return Stream.of(consulta.mantenimientoPreventivo(),
                        consulta.mantenimientosProlongados(hoy.minusDays(parametros.diasMantenimientoProlongado()), hoy),
                        consulta.modulosDefectuosos(),
                        consulta.fallasRecurrentes(parametros.correctivosFallasRecurrentes(), hoy.minusYears(1)),
                        consulta.sinUso(hoy.minusDays(parametros.diasSinUso()), hoy))
                .flatMap(List::stream)
                .sorted(AlertaActivo.POR_URGENCIA)
                .toList();
    }
}
