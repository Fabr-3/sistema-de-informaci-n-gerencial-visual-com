package com.visual.backend.finanzas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.dominio.modelo.DatosProveedor;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;

class ReglasProveedoresTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Test
    void normalizaLosDatosYDescartaServiciosRepetidos() {
        Proveedor proveedor = Proveedor.nuevo(new DatosProveedor(" Transportes Illimani S.R.L. ", " ", "  ", null,
                Arrays.asList("Transporte de carga", " transporte de carga ", "", null, "Grúa")), 1, AHORA);

        assertThat(proveedor.getRazonSocial()).isEqualTo("Transportes Illimani S.R.L.");
        assertThat(proveedor.getNit()).isNull();
        assertThat(proveedor.getTelefono()).isNull();
        assertThat(proveedor.getServicios()).containsExactly("Transporte de carga", "Grúa");
    }

    @Test
    void exigeRazonSocialYLiberaElNitAlEliminar() {
        assertThatThrownBy(() -> Proveedor.nuevo(new DatosProveedor(" ", null, null, null, List.of()), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);

        Proveedor proveedor = Proveedor.nuevo(new DatosProveedor("Sonido Andino", "1020304025", null, null,
                List.of()), 1, AHORA);
        proveedor.eliminar(1, AHORA);

        assertThat(proveedor.getNit()).isNull();
        assertThat(proveedor.getFechaEliminacion()).isEqualTo(AHORA);
    }
}
