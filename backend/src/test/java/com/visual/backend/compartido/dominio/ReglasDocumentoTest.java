package com.visual.backend.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.ContactoCliente;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class ReglasDocumentoTest {

    private static final ElementoCatalogo CARNET = new ElementoCatalogo(1, ReglasDocumento.CARNET_IDENTIDAD);
    private static final ElementoCatalogo NIT = new ElementoCatalogo(2, ReglasDocumento.NIT);
    private static final ElementoCatalogo PERSONA = new ElementoCatalogo(1, Cliente.PERSONA_NATURAL);
    private static final ElementoCatalogo EMPRESA = new ElementoCatalogo(2, "Empresa privada");

    @Test
    void elCarnetAdmiteSiglaDeExpedicionYComplemento() {
        assertThatCode(() -> ReglasDocumento.validar(CARNET, "4876512 LP", "1A")).doesNotThrowAnyException();
        assertThatCode(() -> ReglasDocumento.validar(CARNET, "4876512", null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasDocumento.validar(CARNET, "48A6512", null))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> ReglasDocumento.validar(CARNET, "4876512", "1AB"))
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> ReglasDocumento.validar(NIT, "1020304025", "1A"))
                .hasMessageContaining("Solo el carnet");
        assertThatThrownBy(() -> ReglasDocumento.validar(NIT, "10-20", null)).hasMessageContaining("solo números");
        assertThat(ReglasDocumento.normalizarComplemento(" 1a ")).isEqualTo("1A");
    }

    @Test
    void elMismoCarnetConOtroComplementoEsOtroDocumento() {
        DocumentoCliente original = new DocumentoCliente(null, CARNET, "4876512 lp", null);
        DocumentoCliente duplicado = new DocumentoCliente(null, CARNET, "4876512 LP", "1a");

        assertThat(original.clave()).isNotEqualTo(duplicado.clave());
        assertThat(duplicado.descripcion()).isEqualTo("4876512 LP-1A");
    }

    @Test
    void laPersonaNaturalSeNombraSoloConLetras() {
        List<DocumentoCliente> documentos = List.of(new DocumentoCliente(null, CARNET, "4876512", null));

        assertThatThrownBy(() -> Cliente.nuevo(PERSONA, "Juan Pérez 2", null, documentos, List.of(), 1, null))
                .hasMessageContaining("solo admite letras");
        assertThatCode(() -> Cliente.nuevo(PERSONA, "María José O'Connor-Quispe", null, documentos, List.of(), 1,
                null)).doesNotThrowAnyException();
        assertThatCode(() -> Cliente.nuevo(EMPRESA, "Radio 92.5 S.R.L.", null, documentos, List.of(), 1, null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> Cliente.nuevo(EMPRESA, "Radio <script>", null, documentos, List.of(), 1, null))
                .hasMessageContaining("caracteres no permitidos");
    }

    @Test
    void losContactosRespetanElFormatoDeSuTipo() {
        ElementoCatalogo celular = new ElementoCatalogo(1, "Celular");
        ElementoCatalogo correo = new ElementoCatalogo(3, "Correo electrónico");

        assertThatCode(() -> new ContactoCliente(null, celular, "71234567", "Gerencia").validar())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new ContactoCliente(null, celular, "71A34567", null).validar())
                .isInstanceOf(ReglaNegocioExcepcion.class);
        assertThatThrownBy(() -> new ContactoCliente(null, correo, "ventas.visual.com", null).validar())
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }
}
