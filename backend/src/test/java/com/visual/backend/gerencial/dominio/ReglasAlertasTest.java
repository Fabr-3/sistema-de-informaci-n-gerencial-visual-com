package com.visual.backend.gerencial.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.gerencial.dominio.modelo.ActivoAfectado;
import com.visual.backend.gerencial.dominio.modelo.Alerta;
import com.visual.backend.gerencial.dominio.modelo.AlertaActivo;
import com.visual.backend.gerencial.dominio.modelo.EstadoAlerta;
import com.visual.backend.gerencial.dominio.modelo.ResumenAlertas;
import com.visual.backend.gerencial.dominio.modelo.Severidad;
import com.visual.backend.gerencial.dominio.modelo.TipoAlerta;

class ReglasAlertasTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final ActivoAfectado PANTALLA = new ActivoAfectado(21, "ABPL-0021", "Absen PL3.9 Pro V2",
            "Pantalla LED");
    private static final AlertaActivo STOCK = AlertaActivo.stockBajo(4, "Cinta gaffer negra", new BigDecimal("4.00"),
            new BigDecimal("6.00"), "rollo");

    @Test
    void lasAlertasNuevasIdentificanSuSituacion() {
        AlertaActivo riesgo = AlertaActivo.reservaEnRiesgo(PANTALLA, 45, 15, "Montaje en el palco",
                LocalDate.of(2027, 2, 12));

        assertThat(riesgo.severidad()).isEqualTo(Severidad.CRITICA);
        assertThat(riesgo.clave()).isEqualTo("45");
        assertThat(riesgo.idEvento()).isEqualTo(15);
        assertThat(riesgo.detalle()).isEqualTo("Reservado para «Montaje en el palco» desde el 12/02/2027 y está "
                + "en mantenimiento");
        assertThat(STOCK.idActivo()).isNull();
        assertThat(STOCK.clave()).isEqualTo("4");
        assertThat(STOCK.detalle()).isEqualTo("Cinta gaffer negra: 4 rollo en stock (mínimo 6)");
        assertThat(List.of(STOCK, riesgo).stream().sorted(AlertaActivo.POR_URGENCIA).toList())
                .containsExactly(riesgo, STOCK);
    }

    @Test
    void soloSeAtiendeUnaAlertaPendienteYConComentario() {
        Alerta alerta = Alerta.generar(STOCK, AHORA);
        assertThatThrownBy(() -> alerta.atender(" ", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);

        alerta.atender(" Compra solicitada a la ferretería ", 1, AHORA);

        assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.ATENDIDA);
        assertThat(alerta.getComentarioAtencion()).isEqualTo("Compra solicitada a la ferretería");
        alerta.resolver(AHORA.plusDays(1));
        assertThatThrownBy(() -> alerta.atender("Otra vez", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("resuelta");
    }

    @Test
    void laMismaSituacionActualizaElDetalleSinCambiarElEstado() {
        Alerta alerta = Alerta.generar(STOCK, AHORA);
        alerta.atender("Pedido en curso", 1, AHORA);

        boolean cambio = alerta.actualizar(AlertaActivo.stockBajo(4, "Cinta gaffer negra", new BigDecimal("2"),
                new BigDecimal("6"), "rollo"));

        assertThat(cambio).isTrue();
        assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.ATENDIDA);
        assertThat(alerta.getDetalle()).startsWith("Cinta gaffer negra: 2 rollo");
        assertThat(alerta.identidad()).isEqualTo(Alerta.identidad(TipoAlerta.STOCK_BAJO, "4"));
        assertThat(ResumenAlertas.de(List.of(STOCK)).advertencias()).isEqualTo(1);
    }
}
