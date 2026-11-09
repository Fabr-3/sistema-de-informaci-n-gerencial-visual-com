package com.visual.backend.logistica.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.DatosPersonal;
import com.visual.backend.logistica.dominio.modelo.DocumentoPersonal;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.modelo.TipoJornada;

class ReglasLogisticaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final LocalDateTime INICIO_EVENTO = LocalDateTime.of(2026, 12, 5, 19, 0);
    private static final LocalDateTime FIN_EVENTO = LocalDateTime.of(2026, 12, 5, 23, 30);
    private static final ElementoCatalogo CI = new ElementoCatalogo(1, "Carnet de identidad");
    private static final ElementoCatalogo SONIDO = new ElementoCatalogo(1, "Sonido");
    private static final ElementoCatalogo TECNICO_SONIDO = new ElementoCatalogo(2, "Técnico de sonido");
    private static final Integer ACTOR = 1;

    private static EventoLogistico evento(EstadoEvento estado) {
        return new EventoLogistico(10, "Concierto aniversario", INICIO_EVENTO, FIN_EVENTO, estado, 3,
                "Concierto aniversario 25 años", "Cervecería del Altiplano S.R.L.", "Teatro al Aire Libre",
                "Av. del Ejército s/n", "La Paz");
    }

    private static Jornada jornada(TipoJornada tipo, LocalDateTime inicio, LocalDateTime fin) {
        return new Jornada(null, tipo, new Periodo(inicio, fin), "Escenario principal");
    }

    private static PersonalTecnico persona(boolean activo) {
        return PersonalTecnico.nuevo(new DatosPersonal(" Juan Carlos ", "Mamani Quispe", "71234567",
                new DocumentoPersonal(CI, "4876512 LP"), List.of(SONIDO, SONIDO), null, activo), ACTOR, AHORA);
    }

    @Test
    void elMontajeTerminaAntesDelEventoYElDesmontajeEmpiezaDespues() {
        EventoLogistico evento = evento(EstadoEvento.PROGRAMADO);
        PlanLogistico plan = PlanLogistico.nuevo(evento, ACTOR, AHORA);

        assertThatThrownBy(() -> plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE,
                INICIO_EVENTO.minusHours(4), INICIO_EVENTO.plusHours(1)), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("antes del inicio");
        assertThatThrownBy(() -> plan.agregarJornada(evento, jornada(TipoJornada.DESMONTAJE,
                FIN_EVENTO.minusHours(1), FIN_EVENTO.plusHours(3)), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("después del fin");
        assertThatThrownBy(() -> plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE,
                INICIO_EVENTO.minusDays(40), INICIO_EVENTO.minusDays(39)), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("30 días");
    }

    @Test
    void variasJornadasAmplianLaVentanaPeroNoPuedenCruzarse() {
        EventoLogistico evento = evento(EstadoEvento.PROGRAMADO);
        PlanLogistico plan = PlanLogistico.nuevo(evento, ACTOR, AHORA);
        LocalDateTime primerMontaje = INICIO_EVENTO.minusDays(2).withHour(8);
        plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE, INICIO_EVENTO.minusDays(1).withHour(8),
                INICIO_EVENTO.minusDays(1).withHour(18)), ACTOR, AHORA);
        plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE, primerMontaje, primerMontaje.plusHours(10)),
                ACTOR, AHORA);
        plan.agregarJornada(evento, jornada(TipoJornada.DESMONTAJE, FIN_EVENTO.plusMinutes(30),
                FIN_EVENTO.plusHours(5)), ACTOR, AHORA);

        assertThat(plan.getJornadas()).extracting(jornada -> jornada.periodo().inicio())
                .first().isEqualTo(primerMontaje);
        assertThat(plan.ventana(evento)).isEqualTo(new Periodo(primerMontaje, FIN_EVENTO.plusHours(5)));
        assertThatThrownBy(() -> plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE,
                primerMontaje.plusHours(2), primerMontaje.plusHours(4)), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("se cruza");
    }

    @Test
    void laAsignacionDebeQuedarDentroDelPlanYSoloParaPersonalActivo() {
        EventoLogistico evento = evento(EstadoEvento.PROGRAMADO);
        PlanLogistico plan = PlanLogistico.nuevo(evento, ACTOR, AHORA);
        plan.agregarJornada(evento, jornada(TipoJornada.MONTAJE, INICIO_EVENTO.minusHours(10),
                INICIO_EVENTO.minusHours(2)), ACTOR, AHORA);
        Periodo montajeYFuncion = new Periodo(INICIO_EVENTO.minusHours(10), FIN_EVENTO);

        Asignacion asignacion = Asignacion.nueva(plan, evento, persona(true), TECNICO_SONIDO, montajeYFuncion,
                ACTOR, AHORA);

        assertThat(asignacion.getPersonal().nombreCompleto()).isEqualTo("Juan Carlos Mamani Quispe");
        assertThatThrownBy(() -> Asignacion.nueva(plan, evento, persona(true), TECNICO_SONIDO,
                new Periodo(INICIO_EVENTO.minusHours(12), FIN_EVENTO), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("dentro del plan");
        assertThatThrownBy(() -> Asignacion.nueva(plan, evento, persona(false), TECNICO_SONIDO, montajeYFuncion,
                ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("inactivo");
    }

    @Test
    void laLogisticaDeUnEventoEjecutadoOCanceladoNoSeModifica() {
        EventoLogistico ejecutado = evento(EstadoEvento.EJECUTADO);

        assertThatThrownBy(() -> PlanLogistico.nuevo(ejecutado, ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("ejecutado");
        assertThatThrownBy(() -> PlanLogistico.nuevo(evento(EstadoEvento.CANCELADO), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("cancelado");
    }

    @Test
    void siElEventoCambiaDeHorarioLasJornadasDesfasadasSeAvisan() {
        EventoLogistico original = evento(EstadoEvento.PROGRAMADO);
        PlanLogistico plan = PlanLogistico.nuevo(original, ACTOR, AHORA);
        plan.agregarJornada(original, jornada(TipoJornada.MONTAJE, INICIO_EVENTO.minusHours(6),
                INICIO_EVENTO.minusHours(1)), ACTOR, AHORA);
        EventoLogistico adelantado = new EventoLogistico(10, "Concierto aniversario", INICIO_EVENTO.minusHours(3),
                FIN_EVENTO, EstadoEvento.PROGRAMADO, 3, "Concierto", "Cervecería", "Teatro", null, "La Paz");

        assertThat(plan.avisos(original)).isEmpty();
        assertThat(plan.avisos(adelantado)).singleElement().asString().contains("montaje");
    }

    @Test
    void elPersonalNormalizaSusDatos() {
        PersonalTecnico personal = persona(true);

        assertThat(personal.getNombres()).isEqualTo("Juan Carlos");
        assertThat(personal.getEspecialidades()).containsExactly(SONIDO);
        assertThatThrownBy(() -> PersonalTecnico.nuevo(new DatosPersonal("Ana", " ", null,
                new DocumentoPersonal(CI, "123"), List.of(), null, true), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }
}
