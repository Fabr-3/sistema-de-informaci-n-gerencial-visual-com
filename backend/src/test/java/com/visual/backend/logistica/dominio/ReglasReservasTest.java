package com.visual.backend.logistica.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.logistica.dominio.modelo.ActivoReservable;
import com.visual.backend.logistica.dominio.modelo.DisponibilidadModelo;
import com.visual.backend.logistica.dominio.modelo.EstadoEventoLogistico;
import com.visual.backend.logistica.dominio.modelo.EstadoReserva;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.FaltanteModelo;
import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.PedidoModelo;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.PeriodoReserva;
import com.visual.backend.logistica.dominio.modelo.ReservaActivo;
import com.visual.backend.logistica.dominio.modelo.ReservaAutomatica;
import com.visual.backend.logistica.dominio.modelo.TipoJornada;

class ReglasReservasTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 9, 0);
    private static final LocalDateTime INICIO_EVENTO = LocalDateTime.of(2026, 12, 5, 19, 0);
    private static final LocalDateTime FIN_EVENTO = LocalDateTime.of(2026, 12, 5, 23, 30);
    private static final EventoLogistico EVENTO = new EventoLogistico(10, "Concierto aniversario", INICIO_EVENTO,
            FIN_EVENTO, EstadoEventoLogistico.PROGRAMADO, 3, "Concierto aniversario 25 años",
            "Cervecería del Altiplano S.R.L.", "Teatro al Aire Libre", "Av. del Ejército s/n", "La Paz");
    private static final ActivoReservable PANTALLA = new ActivoReservable(7, "ABPL-0007", "Absen PL3.9",
            "Pantalla LED", true, true, false);
    private static final Integer ACTOR = 1;

    private static Jornada jornada(TipoJornada tipo, LocalDateTime inicio, LocalDateTime fin) {
        return new Jornada(null, tipo, new Periodo(inicio, fin), null);
    }

    @Test
    void sinJornadasElPeriodoEsElDelEvento() {
        PeriodoReserva periodo = PeriodoReserva.calcular(EVENTO, List.of());

        assertThat(periodo).isEqualTo(new PeriodoReserva(INICIO_EVENTO, FIN_EVENTO));
    }

    @Test
    void elPeriodoVaDelMontajeMasTempranoAlDesmontajeMasTardio() {
        LocalDateTime primerMontaje = INICIO_EVENTO.minusDays(1);
        LocalDateTime ultimoDesmontaje = FIN_EVENTO.plusDays(1);
        List<Jornada> jornadas = List.of(
                jornada(TipoJornada.MONTAJE, INICIO_EVENTO.minusHours(6), INICIO_EVENTO.minusHours(1)),
                jornada(TipoJornada.MONTAJE, primerMontaje, primerMontaje.plusHours(8)),
                jornada(TipoJornada.DESMONTAJE, FIN_EVENTO.plusMinutes(30), FIN_EVENTO.plusHours(3)),
                jornada(TipoJornada.DESMONTAJE, ultimoDesmontaje.minusHours(4), ultimoDesmontaje));

        assertThat(PeriodoReserva.calcular(EVENTO, jornadas))
                .isEqualTo(new PeriodoReserva(primerMontaje, ultimoDesmontaje));
        assertThat(PeriodoReserva.calcular(EVENTO, jornadas.subList(0, 2)))
                .isEqualTo(new PeriodoReserva(primerMontaje, FIN_EVENTO));
    }

    @Test
    void detectaLaSuperposicionDePeriodosSemiabiertos() {
        PeriodoReserva periodo = new PeriodoReserva(INICIO_EVENTO, FIN_EVENTO);

        assertThat(periodo.seSuperponeCon(new PeriodoReserva(FIN_EVENTO.minusMinutes(1), FIN_EVENTO.plusHours(1))))
                .isTrue();
        assertThat(periodo.seSuperponeCon(new PeriodoReserva(FIN_EVENTO, FIN_EVENTO.plusHours(1)))).isFalse();
        assertThat(periodo.seSuperponeCon(new PeriodoReserva(INICIO_EVENTO.minusHours(2), INICIO_EVENTO)))
                .isFalse();
        assertThat(periodo.incluye(INICIO_EVENTO)).isTrue();
        assertThat(periodo.incluye(FIN_EVENTO)).isFalse();
        assertThatThrownBy(() -> new PeriodoReserva(FIN_EVENTO, INICIO_EVENTO))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void soloSeReservanEquiposPorUnidadVigentesYFueraDeMantenimiento() {
        PeriodoReserva periodo = new PeriodoReserva(INICIO_EVENTO, FIN_EVENTO);
        ActivoReservable lote = new ActivoReservable(8, "CAB-01", "Cable DMX", "Energía", false, true, false);
        ActivoReservable vendido = new ActivoReservable(9, "ABPL-0009", "Absen PL3.9", "Pantalla LED", true, false,
                false);
        ActivoReservable enTaller = new ActivoReservable(10, "ABPL-0010", "Absen PL3.9", "Pantalla LED", true, true,
                true);

        for (ActivoReservable activo : List.of(lote, vendido, enTaller)) {
            assertThatThrownBy(() -> ReservaActivo.nueva(1, activo, periodo, ACTOR, AHORA))
                    .isInstanceOf(ReglaNegocioExcepcion.class);
        }
        assertThat(ReservaActivo.nueva(1, PANTALLA, periodo, ACTOR, AHORA).getEstado())
                .isEqualTo(EstadoReserva.ACTIVA);
    }

    @Test
    void laReservaLiberadaGuardaElMotivoYNoSeReprograma() {
        ReservaActivo reserva = ReservaActivo.nueva(1, PANTALLA, new PeriodoReserva(INICIO_EVENTO, FIN_EVENTO),
                ACTOR, AHORA);
        assertThatThrownBy(() -> reserva.liberar(" ", ACTOR, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);

        reserva.liberar(" Cliente redujo la pantalla ", ACTOR, AHORA);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(reserva.getMotivoLiberacion()).isEqualTo("Cliente redujo la pantalla");
        assertThat(reserva.getFechaLiberacion()).isEqualTo(AHORA);
        assertThatThrownBy(() -> reserva.reprogramar(new PeriodoReserva(INICIO_EVENTO, FIN_EVENTO.plusHours(1)),
                ACTOR, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void laReservaAutomaticaInformaSoloLosModelosQueNoAlcanzan() {
        List<PedidoModelo> pedidos = List.of(new PedidoModelo(1, 4), new PedidoModelo(2, 2), new PedidoModelo(3, 1));
        List<DisponibilidadModelo> libres = List.of(new DisponibilidadModelo(1, "Novastar VX1000", "Video", 4),
                new DisponibilidadModelo(2, "Shure SM58", "Audio", 1));

        List<FaltanteModelo> faltantes = ReservaAutomatica.faltantes(pedidos, libres, id -> "Modelo " + id);

        assertThat(faltantes).extracting(FaltanteModelo::modelo, FaltanteModelo::faltante)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Shure SM58", 1),
                        org.assertj.core.groups.Tuple.tuple("Modelo 3", 1));
        assertThatThrownBy(() -> ReservaAutomatica.validar(List.of(new PedidoModelo(1, 0))))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> ReservaAutomatica.validar(List.of(new PedidoModelo(1, 1), new PedidoModelo(1, 2))))
                .hasMessageContaining("una sola vez");
    }
}
