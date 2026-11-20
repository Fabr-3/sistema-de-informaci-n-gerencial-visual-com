package com.visual.backend.finanzas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.finanzas.dominio.modelo.DatosProveedor;
import com.visual.backend.finanzas.dominio.modelo.DocumentoProveedor;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;

class ReglasProveedoresTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final ElementoCatalogo EMPRESA = new ElementoCatalogo(2, "Empresa privada");
    private static final ElementoCatalogo PERSONA = new ElementoCatalogo(1, Proveedor.PERSONA_NATURAL);
    private static final ElementoCatalogo NIT = new ElementoCatalogo(2, "NIT");
    private static final ElementoCatalogo CARNET = new ElementoCatalogo(1, "Carnet de identidad");

    private static DatosProveedor datos(ElementoCatalogo tipo, String nombre, List<DocumentoProveedor> documentos) {
        return new DatosProveedor(tipo, nombre, " ", documentos, "  ", null, List.of());
    }

    @Test
    void normalizaLosDatosYDescartaServiciosRepetidos() {
        Proveedor proveedor = Proveedor.nuevo(new DatosProveedor(EMPRESA, " Transportes Illimani S.R.L. ", " ",
                List.of(), "  ", null, Arrays.asList("Transporte de carga", " transporte de carga ", "", null,
                        "Grúa")), 1, AHORA);

        assertThat(proveedor.getRazonSocial()).isEqualTo("Transportes Illimani S.R.L.");
        assertThat(proveedor.getDocumentos()).isEmpty();
        assertThat(proveedor.getDireccion()).isNull();
        assertThat(proveedor.getTelefono()).isNull();
        assertThat(proveedor.getServicios()).containsExactly("Transporte de carga", "Grúa");
        assertThat(proveedor.isActivo()).isTrue();
    }

    @Test
    void validaElNombreYLosDocumentosComoEnLosClientes() {
        assertThatThrownBy(() -> Proveedor.nuevo(datos(EMPRESA, " ", List.of()), 1, AHORA))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> Proveedor.nuevo(datos(PERSONA, "Juan Mamani 2", List.of()), 1, AHORA))
                .hasMessageContaining("solo admite letras");
        assertThatThrownBy(() -> Proveedor.nuevo(datos(EMPRESA, "Sonido Andino",
                List.of(new DocumentoProveedor(null, NIT, "1020", null))), 1, AHORA))
                .hasMessageContaining("solo números");

        Proveedor persona = Proveedor.nuevo(datos(PERSONA, "Juan Mamani",
                List.of(new DocumentoProveedor(null, CARNET, "4876512 lp", "1a"))), 1, AHORA);
        assertThat(persona.documentoPrincipal()).isEqualTo("Carnet de identidad 4876512 LP-1A");
    }

    @Test
    void unProveedorAnuladoNoSeUsaYAlEliminarseLiberaSusDocumentos() {
        Proveedor proveedor = Proveedor.nuevo(datos(EMPRESA, "Sonido Andino",
                List.of(new DocumentoProveedor(null, NIT, "1020304025", null))), 1, AHORA);

        proveedor.cambiarActivo(false, 1, AHORA);

        assertThatThrownBy(proveedor::exigirActivo).hasMessageContaining("anulado");
        assertThatThrownBy(() -> proveedor.cambiarActivo(false, 1, AHORA)).isInstanceOf(ReglaNegocioExcepcion.class);
        proveedor.eliminar(1, AHORA);
        assertThat(proveedor.getDocumentos()).isEmpty();
        assertThat(proveedor.getFechaEliminacion()).isEqualTo(AHORA);
    }
}
