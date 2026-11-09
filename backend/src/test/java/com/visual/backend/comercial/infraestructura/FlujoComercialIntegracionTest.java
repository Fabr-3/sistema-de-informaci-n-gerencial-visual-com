package com.visual.backend.comercial.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.comercial.aplicacion.servicio.ServicioCotizaciones;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujo comercial contra PostgreSQL (base de pruebas): cliente, cotizacion,
 * aceptacion por gerencia, proyecto con eventos, contrato y documentos PDF.
 * Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoComercialIntegracionTest {

    private static final int ID_ROL_GERENTE = 2;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String CABECERA_PDF = "%PDF";
    private static final String RUTA_FIRMADO = "/api/comercial/proyectos/{id}/contratos/{idContrato}/firmado";
    private static final byte[] PDF_FIRMADO = "%PDF-1.4 contrato firmado".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private ServicioCotizaciones servicioCotizaciones;

    private String token;
    private int idServicio;
    private int idLugar;
    private int idCliente;
    private LocalDate hoy;

    @BeforeEach
    void prepararDatos() throws Exception {
        token = iniciarSesion("admin", "admin123456");
        hoy = LocalDate.now();
        idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Pantalla LED exterior de prueba",
                "unidadMedida", "m² por día", "precioReferencial", 180, "activo", true));
        idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Hotel de prueba",
                "direccion", "Av. Arce 2177", "ciudad", "La Paz"));
        idCliente = crear("/api/comercial/clientes", cliente("Banco de prueba S.A.", "990011001"));
    }

    @Test
    void cotizacionAceptadaGeneraProyectoConEventosYContrato() throws Exception {
        JsonNode cotizacion = cuerpo(ejecutar(post("/api/comercial/cotizaciones"), cotizacion(hoy.plusDays(15)))
                .andExpect(status().isCreated()));
        int idCotizacion = cotizacion.get("id").asInt();
        assertThat(cotizacion.get("numero").asString()).isEqualTo("COT-" + hoy.getYear() + "-0001");
        assertThat(cotizacion.get("importeTotal").decimalValue()).isEqualByComparingTo("4220.00");
        assertThat(pdf(get("/api/comercial/cotizaciones/{id}/pdf", idCotizacion))).startsWith(CABECERA_PDF);

        JsonNode aceptada = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion))).andExpect(status().isOk()));
        assertThat(aceptada.get("estado").asString()).isEqualTo("ACEPTADA");
        assertThat(aceptada.get("resueltoPor").asString()).isEqualTo("Administrador Sistema");
        int idProyecto = aceptada.get("idProyecto").asInt();
        ejecutar(put("/api/comercial/cotizaciones/{id}", idCotizacion), cotizacion(hoy.plusDays(20)))
                .andExpect(status().isUnprocessableContent());

        JsonNode detalle = leer(get("/api/comercial/proyectos/{id}", idProyecto));
        assertThat(detalle.get("proyecto").get("estado").asString()).isEqualTo("PLANIFICACION");
        assertThat(detalle.get("proyecto").get("fechaInicio").asString()).isEqualTo(hoy.plusDays(10).toString());

        String inicio = hoy.plusDays(10) + "T08:00:00";
        ejecutar(post("/api/comercial/proyectos/{id}/eventos", idProyecto), evento(inicio, hoy.plusDays(10) + "T18:00:00"))
                .andExpect(status().isCreated());
        ejecutar(post("/api/comercial/proyectos/{id}/eventos", idProyecto),
                evento(hoy.plusDays(30) + "T08:00:00", hoy.plusDays(30) + "T10:00:00"))
                .andExpect(status().isUnprocessableContent());
        ejecutar(put("/api/comercial/proyectos/{id}", idProyecto), Map.of("nombre", "Congreso",
                "fechaInicio", hoy.plusDays(11).toString(), "fechaFin", hoy.plusDays(12).toString()))
                .andExpect(status().isUnprocessableContent());

        JsonNode calendario = leer(get("/api/comercial/cronograma")
                .param("desde", hoy.plusDays(9) + "T00:00:00").param("hasta", hoy.plusDays(12) + "T00:00:00"));
        assertThat(calendario).anySatisfy(fila -> assertThat(fila.get("cliente").asString())
                .isEqualTo("Banco de prueba S.A."));

        JsonNode contrato = cuerpo(ejecutar(post("/api/comercial/proyectos/{id}/contratos", idProyecto),
                Map.of("fechaFirma", hoy.toString(), "terminos", "Anticipo del 40 %")).andExpect(status().isCreated()));
        assertThat(contrato.get("numero").asString()).isEqualTo("CTR-" + hoy.getYear() + "-0001");
        assertThat(pdf(get("/api/comercial/proyectos/{id}/contratos/{idContrato}/pdf", idProyecto,
                contrato.get("id").asInt()))).startsWith(CABECERA_PDF);

        ejecutar(patch("/api/comercial/proyectos/{id}/estado", idProyecto), Map.of("estado", "FINALIZADO"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(autorizado(delete("/api/comercial/clientes/{id}", idCliente)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void elContratoFirmadoSoloAdmitePdfYSeReemplaza() throws Exception {
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion(hoy.plusDays(15)));
        int idProyecto = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion))).andExpect(status().isOk())).get("idProyecto").asInt();
        JsonNode contrato = cuerpo(ejecutar(post("/api/comercial/proyectos/{id}/contratos", idProyecto),
                Map.of("fechaFirma", hoy.toString())).andExpect(status().isCreated()));
        int idContrato = contrato.get("id").asInt();
        assertThat(contrato.get("archivoFirmado").isNull()).isTrue();
        mvc.perform(autorizado(get(RUTA_FIRMADO, idProyecto, idContrato))).andExpect(status().isNotFound());

        adjuntar(idProyecto, idContrato, "imagen escaneada".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isUnprocessableContent());
        adjuntar(idProyecto, idContrato, PDF_FIRMADO).andExpect(status().isOk());
        JsonNode reemplazado = cuerpo(adjuntar(idProyecto, idContrato, PDF_FIRMADO).andExpect(status().isOk()));

        assertThat(reemplazado.get("archivoFirmado").asString()).isEqualTo(contrato.get("numero").asString() + ".pdf");
        assertThat(pdf(get(RUTA_FIRMADO, idProyecto, idContrato))).startsWith(CABECERA_PDF);
    }

    @Test
    void rechazoConMotivoYVencimientoAutomatico() throws Exception {
        int idRechazada = crear("/api/comercial/cotizaciones", cotizacion(hoy.plusDays(15)));
        ejecutar(post("/api/comercial/cotizaciones/{id}/rechazo", idRechazada), Map.of("motivo", " "))
                .andExpect(status().isBadRequest());
        assertThat(cuerpo(ejecutar(post("/api/comercial/cotizaciones/{id}/rechazo", idRechazada),
                Map.of("motivo", "El cliente eligió otro proveedor")).andExpect(status().isOk()))
                .get("estado").asString()).isEqualTo("RECHAZADA");

        Map<String, Object> caducada = cotizacion(hoy.minusDays(1));
        caducada.put("fechaEmision", hoy.minusDays(10).toString());
        int idCaducada = crear("/api/comercial/cotizaciones", caducada);
        mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion", idCaducada)))
                .andExpect(status().isUnprocessableContent());

        servicioCotizaciones.vencerCaducadas();

        assertThat(leer(get("/api/comercial/cotizaciones/{id}", idCaducada)).get("estado").asString())
                .isEqualTo("VENCIDA");
    }

    @Test
    void losDocumentosDeClientesSonUnicosYSeConservanAlEditar() throws Exception {
        ejecutar(post("/api/comercial/clientes"), cliente("Otra empresa", "990011001"))
                .andExpect(status().isConflict());

        JsonNode actual = leer(get("/api/comercial/clientes/{id}", idCliente));
        Map<String, Object> edicion = cliente("Banco de prueba S.A.", "990011001");
        edicion.put("direccion", "Av. Camacho 1277, piso 3");
        edicion.put("documentos", List.of(Map.of("id", actual.get("documentos").get(0).get("id").asInt(),
                "idTipoDocumento", idNit(), "numero", "990011001")));
        ejecutar(put("/api/comercial/clientes/{id}", idCliente), edicion).andExpect(status().isOk());

        assertThat(leer(get("/api/comercial/clientes").param("texto", "990011")).get("totalElementos").asInt())
                .isEqualTo(1);
    }

    @Test
    void elGerenteApruebaPeroNoRegistraCotizaciones() throws Exception {
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion(hoy.plusDays(15)));
        crear("/api/usuarios", Map.of("nombreUsuario", "gerente.comercial", "nombres", "Gerente",
                "apellidoPaterno", "Comercial", "email", "gerente.comercial@visual.com.bo", "idRol", ID_ROL_GERENTE,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenGerente = cambiarContrasena(iniciarSesion("gerente.comercial", CONTRASENA_TEMPORAL));

        mvc.perform(post("/api/comercial/cotizaciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cotizacion(hoy.plusDays(15)))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/comercial/cotizaciones/{id}/aceptacion", idCotizacion)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGerente))
                .andExpect(status().isOk());
    }

    private Map<String, Object> cliente(String razonSocial, String nit) throws Exception {
        Map<String, Object> cliente = new HashMap<>();
        cliente.put("idTipoCliente", idTipo("/api/comercial/catalogos/tipos-cliente", "Empresa privada"));
        cliente.put("razonSocial", razonSocial);
        cliente.put("documentos", List.of(Map.of("idTipoDocumento", idNit(), "numero", nit)));
        cliente.put("contactos", List.of(Map.of("idTipoContacto",
                idTipo("/api/comercial/catalogos/tipos-contacto", "Celular"), "valor", "71234567",
                "referencia", "Gerencia de marketing")));
        return cliente;
    }

    private Map<String, Object> cotizacion(LocalDate validez) {
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Congreso anual de prueba");
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", validez.toString());
        cotizacion.put("fechaEventoInicio", hoy.plusDays(10).toString());
        cotizacion.put("fechaEventoFin", hoy.plusDays(11).toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 24, "precioUnitario", 180,
                "descuento", 100)));
        return cotizacion;
    }

    private Map<String, Object> evento(String inicio, String fin) {
        return Map.of("idLugar", idLugar, "nombre", "Inauguración", "fechaInicio", inicio, "fechaFin", fin);
    }

    private int idNit() throws Exception {
        return idTipo("/api/comercial/catalogos/tipos-documento", "NIT");
    }

    private int idTipo(String ruta, String nombre) throws Exception {
        for (JsonNode tipo : leer(get(ruta))) {
            if (tipo.get("nombre").asString().equals(nombre)) {
                return tipo.get("id").asInt();
            }
        }
        throw new IllegalStateException("No existe el tipo " + nombre);
    }

    private int crear(String ruta, Map<String, ?> datos) throws Exception {
        return cuerpo(ejecutar(post(ruta), datos).andExpect(status().isCreated())).get("id").asInt();
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> datos) throws Exception {
        return mvc.perform(autorizado(solicitud).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos)));
    }

    private ResultActions adjuntar(int idProyecto, int idContrato, byte[] contenido) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "contrato.pdf",
                MediaType.APPLICATION_PDF_VALUE, contenido);
        return mvc.perform(multipart(HttpMethod.PUT, RUTA_FIRMADO, idProyecto, idContrato).file(archivo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud) throws Exception {
        return cuerpo(mvc.perform(autorizado(solicitud)).andExpect(status().isOk()));
    }

    private String pdf(MockHttpServletRequestBuilder solicitud) throws Exception {
        byte[] contenido = mvc.perform(autorizado(solicitud)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new String(contenido, 0, CABECERA_PDF.length(), StandardCharsets.ISO_8859_1);
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String iniciarSesion(String usuario, String contrasena) throws Exception {
        return cuerpo(mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", usuario, "contrasena", contrasena))))
                .andExpect(status().isOk())).get("tokenAcceso").asString();
    }

    private String cambiarContrasena(String tokenTemporal) throws Exception {
        return cuerpo(mvc.perform(put("/api/auth/contrasena").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTemporal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("contrasenaActual", CONTRASENA_TEMPORAL,
                                "nuevaContrasena", "definitiva123"))))
                .andExpect(status().isOk())).get("tokenAcceso").asString();
    }
}
