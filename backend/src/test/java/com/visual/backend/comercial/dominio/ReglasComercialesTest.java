package com.visual.backend.comercial.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.DatosCotizacion;
import com.visual.backend.comercial.dominio.modelo.DatosEvento;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.EstadoCotizacion;
import com.visual.backend.comercial.dominio.modelo.EstadoEvento;
import com.visual.backend.comercial.dominio.modelo.EstadoProyecto;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.LineaCotizacion;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.modelo.ResumenCliente;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class ReglasComercialesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final ResumenCliente CLIENTE = new ResumenCliente(1, "Banco Andino S.A.");
    private static final LineaCotizacion PANTALLA = new LineaCotizacion(1, "Pantalla LED exterior", "m² por día", 24,
            new BigDecimal("180.00"), new BigDecimal("320.00"));
    private static final LineaCotizacion SONIDO = new LineaCotizacion(2, "Sistema de sonido", "jornada", 2,
            new BigDecimal("4500.00"), BigDecimal.ZERO);
    private static final Lugar LUGAR = new Lugar(1, "Hotel Casa Grande", "Av. Ballivián 958", "La Paz");

    private static DatosCotizacion datos(LocalDate validez, List<LineaCotizacion> lineas) {
        return new DatosCotizacion(CLIENTE, " Congreso anual ", HOY.minusDays(2), validez, HOY.plusDays(10),
                HOY.plusDays(11), null, lineas);
    }

    private static Cotizacion borrador() {
        return Cotizacion.nueva("COT-2026-0001", datos(HOY.plusDays(15), List.of(PANTALLA, SONIDO)), 1, AHORA);
    }

    @Test
    void calculaSubtotalesYTotalConDescuento() {
        Cotizacion cotizacion = borrador();

        assertThat(PANTALLA.subtotal()).isEqualByComparingTo("4000.00");
        assertThat(cotizacion.importeTotal()).isEqualByComparingTo("13000.00");
        assertThat(cotizacion.getTitulo()).isEqualTo("Congreso anual");
        assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.BORRADOR);
    }

    @Test
    void validaLineasYFechas() {
        assertThatThrownBy(() -> new LineaCotizacion(1, "Pantalla", "día", 1, BigDecimal.TEN, new BigDecimal("11")))
                .hasMessageContaining("supera");
        assertThatThrownBy(() -> Cotizacion.nueva("COT-1", datos(HOY.plusDays(5), List.of()), 1, AHORA))
                .hasMessageContaining("al menos un servicio");
        assertThatThrownBy(() -> Cotizacion.nueva("COT-1", datos(HOY.minusDays(5), List.of(PANTALLA)), 1, AHORA))
                .hasMessageContaining("validez");
        assertThatThrownBy(() -> Cotizacion.nueva("COT-1", datos(HOY.plusDays(5), List.of(PANTALLA, PANTALLA)), 1,
                AHORA)).hasMessageContaining("repetido");
    }

    @Test
    void unaCotizacionAceptadaNoSeModificaYGeneraProyecto() {
        Cotizacion cotizacion = borrador();

        cotizacion.aceptar(5, AHORA);
        Proyecto proyecto = Proyecto.desdeCotizacion(cotizacion, 5, AHORA);

        assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.ACEPTADA);
        assertThat(cotizacion.getResueltoPor()).isEqualTo(5);
        assertThat(proyecto.getNombre()).isEqualTo("Congreso anual");
        assertThat(proyecto.getFechaInicio()).isEqualTo(HOY.plusDays(10));
        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.PLANIFICACION);
        assertThatThrownBy(() -> cotizacion.actualizar(datos(HOY.plusDays(15), List.of(PANTALLA)), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> cotizacion.rechazar("Precio", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void elRechazoExigeMotivoYLaVencidaNoSeAcepta() {
        Cotizacion cotizacion = borrador();
        assertThatThrownBy(() -> cotizacion.rechazar(" ", 1, AHORA)).hasMessageContaining("motivo");

        Cotizacion caducada = Cotizacion.nueva("COT-2026-0002", datos(HOY.minusDays(1), List.of(SONIDO)), 1,
                AHORA.minusDays(3));
        assertThatThrownBy(() -> caducada.aceptar(1, AHORA)).hasMessageContaining("venció");
        assertThat(caducada.vencerSiCorresponde(HOY)).isTrue();
        assertThat(caducada.getEstado()).isEqualTo(EstadoCotizacion.VENCIDA);
        assertThat(caducada.vencerSiCorresponde(HOY)).isFalse();
    }

    @Test
    void elProyectoRespetaSusTransiciones() {
        Cotizacion cotizacion = borrador();
        cotizacion.aceptar(1, AHORA);
        Proyecto proyecto = Proyecto.desdeCotizacion(cotizacion, 1, AHORA);

        assertThatThrownBy(() -> proyecto.cambiarEstado(EstadoProyecto.FINALIZADO, 1, AHORA))
                .hasMessageContaining("no puede pasar");
        proyecto.cambiarEstado(EstadoProyecto.EN_EJECUCION, 1, AHORA);
        proyecto.cambiarEstado(EstadoProyecto.FINALIZADO, 1, AHORA);
        assertThatThrownBy(() -> proyecto.actualizar("Otro", null, null, null, 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void losEventosQuedanDentroDelProyectoYSoloSeEjecutanUnaVezIniciados() {
        Cotizacion cotizacion = borrador();
        cotizacion.aceptar(1, AHORA);
        Proyecto proyecto = Proyecto.desdeCotizacion(cotizacion, 1, AHORA);
        LocalDateTime inicio = HOY.plusDays(10).atTime(8, 0);

        assertThatThrownBy(() -> Evento.nuevo(proyecto, new DatosEvento(LUGAR, "Montaje", inicio.minusDays(2),
                inicio), 1, AHORA)).hasMessageContaining("periodo del proyecto");
        assertThatThrownBy(() -> Evento.nuevo(proyecto, new DatosEvento(LUGAR, "Inauguración", inicio, inicio), 1,
                AHORA)).hasMessageContaining("posterior");

        Evento evento = Evento.nuevo(proyecto, new DatosEvento(LUGAR, "Inauguración", inicio, inicio.plusHours(6)),
                1, AHORA);
        assertThatThrownBy(() -> evento.marcarEjecutado(1, AHORA)).hasMessageContaining("aún no ha comenzado");
        evento.cancelar(1, AHORA);
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.CANCELADO);
        assertThatThrownBy(() -> evento.cancelar(1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void elClienteRequiereDocumentosSinRepetir() {
        ElementoCatalogo empresa = new ElementoCatalogo(2, "Empresa privada");
        ElementoCatalogo nit = new ElementoCatalogo(2, "NIT");

        assertThatThrownBy(() -> Cliente.nuevo(empresa, "Banco", null, List.of(), List.of(), 1, AHORA))
                .hasMessageContaining("al menos un documento");
        assertThatThrownBy(() -> Cliente.nuevo(empresa, "Banco", null,
                List.of(new DocumentoCliente(null, nit, "1020"), new DocumentoCliente(null, nit, " 1020 ")),
                List.of(), 1, AHORA)).hasMessageContaining("repetido");
    }
}
