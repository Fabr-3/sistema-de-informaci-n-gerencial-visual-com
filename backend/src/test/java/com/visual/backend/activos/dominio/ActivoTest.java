package com.visual.backend.activos.dominio;

import static com.visual.backend.activos.dominio.FabricaPruebasActivos.AHORA;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.BODEGA;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.BODEGA_INACTIVA;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.activo;
import static com.visual.backend.activos.dominio.FabricaPruebasActivos.modelo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.CondicionFinal;
import com.visual.backend.activos.dominio.modelo.DatosActivo;
import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.TipoControl;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class ActivoTest {

    private static final DatosActivo DATOS = new DatosActivo("LED-002", null, null, null, null);

    @Test
    void nuevoQuedaDisponibleConCodigoNormalizado() {
        Activo activo = activo();

        assertThat(activo.getCodigoInventario()).isEqualTo("LED-001");
        assertThat(activo.getEstado()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void noAceptaModelosPorLoteNiBodegasInactivas() {
        assertThatThrownBy(() -> Activo.nuevo(DATOS, modelo(TipoControl.LOTE, null), BODEGA, 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("se controla por lote");
        assertThatThrownBy(() -> Activo.nuevo(DATOS, modelo(TipoControl.UNIDAD, null), BODEGA_INACTIVA, 1, AHORA))
                .hasMessageContaining("inactiva");
    }

    @Test
    void cicloDeMantenimientoDevuelveElActivoADisponible() {
        Activo activo = activo();

        activo.iniciarMantenimiento(1, AHORA);
        assertThat(activo.getEstado()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        assertThatThrownBy(activo::validarPuedeRegistrarUso).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> activo.iniciarMantenimiento(1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);

        activo.finalizarMantenimiento(CondicionFinal.OPERATIVO, null, 1, AHORA);
        assertThat(activo.getEstado()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void unActivoNoReparableQuedaDadoDeBaja() {
        Activo activo = activo();
        activo.iniciarMantenimiento(1, AHORA);

        activo.finalizarMantenimiento(CondicionFinal.NO_REPARABLE, "Placa quemada", 1, AHORA);

        assertThat(activo.estaFueraDeInventario()).isTrue();
        assertThat(activo.getMotivoBaja()).isEqualTo("Placa quemada");
    }

    @Test
    void laBajaExigeMotivoYNoSeRepite() {
        Activo activo = activo();
        assertThatThrownBy(() -> activo.darDeBaja(" ", 1, AHORA)).hasMessageContaining("motivo");

        activo.darDeBaja("Robo", 1, AHORA);

        assertThatThrownBy(() -> activo.darDeBaja("Otra", 1, AHORA)).hasMessageContaining("ya no está en el inventario");
        assertThatThrownBy(() -> activo.iniciarMantenimiento(1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void noSeDaDeBajaUnActivoEnMantenimiento() {
        Activo activo = activo();
        activo.iniciarMantenimiento(1, AHORA);

        assertThatThrownBy(() -> activo.darDeBaja("Falla", 1, AHORA)).hasMessageContaining("Solo se puede dar de baja");
    }

    @Test
    void laVentaSacaAlEquipoDelInventarioYSuAnulacionLoDevuelve() {
        Activo activo = activo();
        activo.iniciarMantenimiento(1, AHORA);
        assertThatThrownBy(() -> activo.vender("Movimiento financiero N.º 9", 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class).hasMessageContaining("disponible");
        activo.finalizarMantenimiento(CondicionFinal.OPERATIVO, null, 1, AHORA);

        activo.vender("Movimiento financiero N.º 9", 1, AHORA);

        assertThat(activo.getEstado()).isEqualTo(EstadoOperativo.VENDIDO);
        assertThat(activo.estaFueraDeInventario()).isTrue();
        assertThat(activo.getObservaciones()).contains("Vendido: Movimiento financiero N.º 9");
        assertThatThrownBy(activo::validarPuedeRegistrarUso).isInstanceOf(ReglaNegocioExcepcion.class);
        activo.revertirVenta("Movimiento financiero N.º 9", 1, AHORA);
        assertThat(activo.getEstado()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThatThrownBy(() -> activo.revertirVenta("Otra", 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
    }
}
