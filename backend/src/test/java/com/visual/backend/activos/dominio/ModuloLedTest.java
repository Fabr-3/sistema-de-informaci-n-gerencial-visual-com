package com.visual.backend.activos.dominio;

import static com.visual.backend.activos.dominio.FabricaPruebasActivos.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.EstadoModuloLed;
import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class ModuloLedTest {

    private static final Integer ID_PANTALLA = 5;

    private static Activo pantalla(Integer id, EstadoOperativo estado) {
        return Activo.builder().id(id).codigoInventario("ABPL-000" + id).estado(estado).build();
    }

    private static ModuloLed modulo(EstadoModuloLed estado) {
        return new ModuloLed(10, ID_PANTALLA, "MOD-A1", estado, AHORA.minusDays(30), null);
    }

    @Test
    void elReemplazoRetiraElModuloYCreaUnoActivoEnLaMismaPantalla() {
        ModuloLed defectuoso = modulo(EstadoModuloLed.DEFECTUOSO);

        ModuloLed nuevo = defectuoso.reemplazar(" mod-b7 ", AHORA);

        assertThat(defectuoso.getEstado()).isEqualTo(EstadoModuloLed.RETIRADO);
        assertThat(nuevo.getCodigo()).isEqualTo("MOD-B7");
        assertThat(nuevo.getEstado()).isEqualTo(EstadoModuloLed.ACTIVO);
        assertThat(nuevo.getIdActivoPadre()).isEqualTo(ID_PANTALLA);
        assertThatThrownBy(() -> defectuoso.reemplazar("MOD-C1", AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> modulo(EstadoModuloLed.ACTIVO).reemplazar("mod-a1", AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void soloUnModuloActivoSeReasignaAOtraPantallaVigente() {
        ModuloLed activo = modulo(EstadoModuloLed.ACTIVO);

        assertThatThrownBy(() -> activo.reasignar(pantalla(ID_PANTALLA, EstadoOperativo.DISPONIBLE), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> activo.reasignar(pantalla(6, EstadoOperativo.BAJA), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> activo.reasignar(pantalla(7, EstadoOperativo.VENDIDO), AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> modulo(EstadoModuloLed.DEFECTUOSO)
                .reasignar(pantalla(8, EstadoOperativo.DISPONIBLE), AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);

        activo.reasignar(pantalla(8, EstadoOperativo.EN_MANTENIMIENTO), AHORA);

        assertThat(activo.getIdActivoPadre()).isEqualTo(8);
        assertThat(activo.getFechaModificacion()).isEqualTo(AHORA);
    }
}
