package com.visual.backend.finanzas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.dominio.modelo.DatosMovimiento;
import com.visual.backend.finanzas.dominio.modelo.EstadoMovimiento;
import com.visual.backend.finanzas.dominio.modelo.ItemVenta;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Naturaleza;
import com.visual.backend.finanzas.dominio.modelo.Referencia;
import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;

class ReglasMovimientosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final TipoMovimiento COBRO = new TipoMovimiento(1, "Cobro de servicios", Naturaleza.INGRESO,
            false, false);
    private static final TipoMovimiento VENTA = new TipoMovimiento(3, "Venta de equipos", Naturaleza.INGRESO,
            false, true);
    private static final TipoMovimiento SUBCONTRATACION = new TipoMovimiento(7, "Subcontratación de servicios",
            Naturaleza.EGRESO, true, false);
    private static final Referencia PROYECTO = new Referencia(3, "Cena de gala de fin de año");
    private static final Referencia PROVEEDOR = new Referencia(2, "Generadores Andinos S.R.L.");

    private static DatosMovimiento datos(TipoMovimiento tipo, String monto, LocalDate fecha, Referencia proveedor,
                                         List<ItemVenta> items) {
        return new DatosMovimiento(tipo, " Anticipo del 50 % ", new BigDecimal(monto), fecha, PROYECTO, null,
                proveedor, " 000123 ", items);
    }

    @Test
    void normalizaElMovimientoYCalculaElSigno() {
        MovimientoFinanciero cobro = MovimientoFinanciero.registrar(datos(COBRO, "9350.5", HOY, null, List.of()),
                1, AHORA);
        MovimientoFinanciero pago = MovimientoFinanciero.registrar(
                datos(SUBCONTRATACION, "1200", HOY, PROVEEDOR, null), 1, AHORA);

        assertThat(cobro.getConcepto()).isEqualTo("Anticipo del 50 %");
        assertThat(cobro.getMonto()).isEqualByComparingTo("9350.50");
        assertThat(cobro.getNumeroFactura()).isEqualTo("000123");
        assertThat(cobro.montoConSigno()).isEqualByComparingTo("9350.50");
        assertThat(pago.montoConSigno()).isEqualByComparingTo("-1200");
    }

    @Test
    void validaMontoFechaYProveedor() {
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(COBRO, "0", HOY, null, null), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("mayor a cero");
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(COBRO, "10", HOY.plusDays(1), null, null),
                1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("futura");
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(SUBCONTRATACION, "10", HOY, null, null),
                1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("requiere");
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(COBRO, "10", HOY, PROVEEDOR, null),
                1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("ingresos");
    }

    @Test
    void soloLasVentasLlevanDetalleSinRepetidos() {
        ItemVenta pantalla = ItemVenta.unidad(15, "ABPL-0003");
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(VENTA, "8000", HOY, null, List.of()), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("Indique");
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(COBRO, "8000", HOY, null, List.of(pantalla)),
                1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> MovimientoFinanciero.registrar(datos(VENTA, "8000", HOY, null,
                List.of(pantalla, pantalla)), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("repetido");

        MovimientoFinanciero venta = MovimientoFinanciero.registrar(datos(VENTA, "8000", HOY, null,
                List.of(pantalla, ItemVenta.lote(4, 10, "Cable XLR"))), 1, AHORA);
        assertThat(venta.esVenta()).isTrue();
    }

    @Test
    void laAnulacionExigeMotivoYNoSeRepite() {
        MovimientoFinanciero cobro = MovimientoFinanciero.registrar(datos(COBRO, "500", HOY, null, null), 1, AHORA);

        assertThatThrownBy(() -> cobro.anular(" ", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
        cobro.anular("Monto duplicado", 1, AHORA);
        assertThat(cobro.getEstado()).isEqualTo(EstadoMovimiento.ANULADO);
        assertThatThrownBy(() -> cobro.anular("Otra vez", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }
}
