package com.visual.backend.subcontratacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.Comprobante;
import com.visual.backend.subcontratacion.dominio.modelo.Consumible;
import com.visual.backend.subcontratacion.dominio.modelo.ConsumoProyecto;
import com.visual.backend.subcontratacion.dominio.modelo.DatosConsumible;
import com.visual.backend.subcontratacion.dominio.modelo.DatosGasto;
import com.visual.backend.subcontratacion.dominio.modelo.DatosSubcontratacion;
import com.visual.backend.subcontratacion.dominio.modelo.GastoEvento;
import com.visual.backend.subcontratacion.dominio.modelo.MovimientoConsumible;
import com.visual.backend.subcontratacion.dominio.modelo.ProyectoReferencia;
import com.visual.backend.subcontratacion.dominio.modelo.Subcontratacion;
import com.visual.backend.subcontratacion.dominio.modelo.TipoMovimientoConsumible;

class ReglasSubcontratacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Integer ACTOR = 1;
    private static final ElementoCatalogo PROYECTO = new ElementoCatalogo(3, "Concierto aniversario 25 años");
    private static final ElementoCatalogo EVENTO = new ElementoCatalogo(10, "Función principal");

    private static Consumible cinta(String stockMinimo) {
        return Consumible.nuevo(new DatosConsumible(" Cinta gaffer negra ", "rollo",
                stockMinimo == null ? null : new BigDecimal(stockMinimo), new BigDecimal("45"), true), ACTOR, AHORA);
    }

    @Test
    void elStockNuncaQuedaNegativo() {
        Consumible consumible = cinta("2");
        consumible.ingresar(new BigDecimal("5"), null, "Compra", ACTOR, AHORA);

        assertThatThrownBy(() -> consumible.consumir(new BigDecimal("6"), "Consumo", ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("disponible 5.00 rollo");
        assertThatThrownBy(() -> consumible.ajustar(new BigDecimal("-5.5"), "Conteo físico", ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> consumible.ajustar(new BigDecimal("-1"), " ", ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);

        MovimientoConsumible consumo = consumible.consumir(new BigDecimal("3"), "Consumo", ACTOR, AHORA);

        assertThat(consumo.tipo()).isEqualTo(TipoMovimientoConsumible.CONSUMO);
        assertThat(consumo.cantidad()).isEqualByComparingTo("-3");
        assertThat(consumible.getStockActual()).isEqualByComparingTo("2");
        assertThat(consumible.stockBajo()).isTrue();
        assertThat(cinta(null).stockBajo()).isFalse();
    }

    @Test
    void elConsumoCopiaElCostoVigenteAlRegistrarse() {
        Consumible consumible = cinta(null);
        consumible.ingresar(new BigDecimal("10"), null, null, ACTOR, AHORA);
        ConsumoProyecto consumo = ConsumoProyecto.registrar(consumible, PROYECTO, EVENTO, new BigDecimal("4"),
                AHORA.toLocalDate(), ACTOR, AHORA);

        consumible.ingresar(new BigDecimal("10"), new BigDecimal("60"), "Compra con precio nuevo", ACTOR, AHORA);

        assertThat(consumo.getCostoUnitario()).isEqualByComparingTo("45");
        assertThat(consumo.costoTotal()).isEqualByComparingTo("180");
        assertThat(consumible.getCostoUnitario()).isEqualByComparingTo("60");
        assertThatThrownBy(() -> ConsumoProyecto.registrar(consumible, PROYECTO, null, BigDecimal.ONE,
                AHORA.toLocalDate().plusDays(1), ACTOR, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void laSubcontratacionExigeCostoPositivoYSeAnulaUnaSolaVez() {
        ElementoCatalogo proveedor = new ElementoCatalogo(7, "Generadores Illimani S.R.L.");
        DatosSubcontratacion datos = new DatosSubcontratacion(proveedor, null, PROYECTO, EVENTO,
                " Generador de 60 kVA con operador ", new BigDecimal("1800"), LocalDate.of(2026, 11, 10));

        Subcontratacion subcontratacion = Subcontratacion.registrar(datos, ACTOR, AHORA);
        subcontratacion.anular("Proveedor no se presentó", ACTOR, AHORA);

        assertThat(subcontratacion.concepto())
                .isEqualTo("Subcontratación · Generadores Illimani S.R.L. · Generador de 60 kVA con operador");
        assertThatThrownBy(() -> subcontratacion.anular("Otra vez", ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> Subcontratacion.registrar(new DatosSubcontratacion(proveedor, null, PROYECTO, null,
                "Generador", BigDecimal.ZERO, LocalDate.of(2026, 11, 10)), ACTOR, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> new ProyectoReferencia(3, "Feria cerrada", false).exigirVigente())
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void elSubalquilerExigeCantidadYEventoYSuConceptoNombraElModelo() {
        ElementoCatalogo proveedor = new ElementoCatalogo(7, "Luces del Sur");
        ElementoCatalogo modelo = new ElementoCatalogo(4, "Robe Pointe");
        LocalDate fecha = LocalDate.of(2026, 11, 10);

        Subcontratacion subalquiler = Subcontratacion.registrar(new DatosSubcontratacion(proveedor, null, PROYECTO,
                EVENTO, "Faltan cabezales", new BigDecimal("600"), fecha, modelo, 2), ACTOR, AHORA);

        assertThat(subalquiler.esSubalquiler()).isTrue();
        assertThat(subalquiler.concepto()).isEqualTo("Subalquiler · Luces del Sur · 2 × Robe Pointe");
        assertThatThrownBy(() -> Subcontratacion.registrar(new DatosSubcontratacion(proveedor, null, PROYECTO, null,
                "Faltan cabezales", new BigDecimal("600"), fecha, modelo, 2), ACTOR, AHORA))
                .hasMessageContaining("evento");
        assertThatThrownBy(() -> Subcontratacion.registrar(new DatosSubcontratacion(proveedor, null, PROYECTO, EVENTO,
                "Faltan cabezales", new BigDecimal("600"), fecha, modelo, null), ACTOR, AHORA))
                .hasMessageContaining("unidades");
    }

    @Test
    void elComprobanteSeValidaPorSuFirmaYElGastoExigeEventoYMontoPositivo() {
        byte[] pdf = "%PDF-1.7 contenido".getBytes();
        Comprobante comprobante = Comprobante.recibido(" factura.pdf ", "application/pdf", pdf);

        assertThat(comprobante.archivo()).matches("gasto-[a-f0-9-]{36}\\.pdf");
        assertThat(comprobante.nombreOriginal()).isEqualTo("factura.pdf");
        assertThatThrownBy(() -> Comprobante.recibido("foto.png", "image/png", pdf))
                .hasMessageContaining("imagen");
        assertThatThrownBy(() -> Comprobante.recibido("vacio.pdf", "application/pdf", new byte[0]))
                .hasMessageContaining("Adjunte");

        LocalDate fecha = LocalDate.of(2026, 11, 10);
        GastoEvento gasto = GastoEvento.registrar(new DatosGasto(PROYECTO, EVENTO, " Pasaje del técnico ",
                new BigDecimal("30"), fecha, comprobante), ACTOR, AHORA);
        assertThat(gasto.concepto()).isEqualTo("Gasto del evento · " + EVENTO.nombre() + " · Pasaje del técnico");
        assertThatThrownBy(() -> GastoEvento.registrar(new DatosGasto(PROYECTO, null, "Pasaje",
                new BigDecimal("30"), fecha, comprobante), ACTOR, AHORA)).hasMessageContaining("evento");
        assertThatThrownBy(() -> GastoEvento.registrar(new DatosGasto(PROYECTO, EVENTO, "Pasaje",
                BigDecimal.ZERO, fecha, comprobante), ACTOR, AHORA)).hasMessageContaining("mayor a cero");
    }
}
