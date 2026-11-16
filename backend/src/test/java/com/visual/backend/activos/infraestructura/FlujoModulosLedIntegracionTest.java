package com.visual.backend.activos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reemplazo, reasignacion e historial de los modulos LED contra PostgreSQL.
 * Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoModulosLedIntegracionTest {

    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String MIGRACION_HISTORIAL = "db/migration/V13__historial_modulos_led.sql";
    private static final String CARGA_INICIAL = "INSERT INTO historial_modulos_led";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private int idPantalla;
    private int idOtraPantalla;
    private String codigoPantalla;
    private String codigoOtraPantalla;

    @BeforeEach
    void prepararPantallas() throws Exception {
        token = iniciarSesion("admin", "admin123456");
        int idBodega = leer(get("/api/activos/catalogos/bodegas")).get(0).get("id").asInt();
        int idCategoria = leer(get("/api/activos/catalogos/categorias")).get(0).get("id").asInt();
        int idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante módulos"));
        Map<String, Object> modelo = new HashMap<>(Map.of("idFabricante", idFabricante, "idCategoria", idCategoria,
                "nombre", "Pantalla módulos", "tipoControl", "UNIDAD", "prefijoCodigo", "MLD"));
        int idModelo = crear("/api/activos/modelos", modelo);
        JsonNode pantallas = cuerpo(ejecutar(post("/api/activos"), Map.of("idModelo", idModelo, "idBodega", idBodega,
                "cantidad", 2)).andExpect(status().isCreated()));
        idPantalla = pantallas.get(0).get("id").asInt();
        codigoPantalla = pantallas.get(0).get("codigoInventario").asString();
        idOtraPantalla = pantallas.get(1).get("id").asInt();
        codigoOtraPantalla = pantallas.get(1).get("codigoInventario").asString();
    }

    @Test
    void reemplazarUnModuloDefectuosoDejaConstanciaEnAmbos() throws Exception {
        int idModulo = agregarModulo(idPantalla, "mld-a1");
        ejecutar(patch(rutaModulo(idPantalla, idModulo) + "/estado"), Map.of("estado", "DEFECTUOSO"))
                .andExpect(status().isOk());

        JsonNode nuevo = cuerpo(ejecutar(post(rutaModulo(idPantalla, idModulo) + "/reemplazo"),
                Map.of("codigoNuevo", "mld-a2", "motivo", "Píxeles muertos")).andExpect(status().isCreated()));

        assertThat(nuevo.get("codigo").asString()).isEqualTo("MLD-A2");
        assertThat(nuevo.get("estado").asString()).isEqualTo("ACTIVO");
        assertThat(leer(get("/api/activos/{id}/modulos", idPantalla)))
                .extracting(modulo -> modulo.get("codigo").asString() + ":" + modulo.get("estado").asString())
                .containsExactly("MLD-A1:RETIRADO", "MLD-A2:ACTIVO");
        JsonNode historial = leer(get(rutaModulo(idPantalla, idModulo) + "/historial"));
        assertThat(historial).extracting(entrada -> entrada.get("tipo").asString())
                .containsExactly("ALTA", "CAMBIO_ESTADO", "REEMPLAZO");
        assertThat(historial.get(2).get("estadoAnterior").asString()).isEqualTo("DEFECTUOSO");
        assertThat(historial.get(2).get("moduloRelacionado").asString()).isEqualTo("MLD-A2");
        assertThat(historial.get(2).get("motivo").asString()).isEqualTo("Píxeles muertos");
        assertThat(leer(get(rutaModulo(idPantalla, nuevo.get("id").asInt()) + "/historial")))
                .singleElement().satisfies(entrada -> {
                    assertThat(entrada.get("tipo").asString()).isEqualTo("REEMPLAZO");
                    assertThat(entrada.get("moduloRelacionado").asString()).isEqualTo("MLD-A1");
                });
    }

    @Test
    void elReemplazoConUnCodigoYaRegistradoSeRechaza() throws Exception {
        int idModulo = agregarModulo(idPantalla, "MLD-B1");
        agregarModulo(idOtraPantalla, "MLD-B2");

        ejecutar(post(rutaModulo(idPantalla, idModulo) + "/reemplazo"),
                Map.of("codigoNuevo", "mld-b2", "motivo", "Golpe en el transporte")).andExpect(status().isConflict());

        assertThat(leer(get("/api/activos/{id}/modulos", idPantalla)).get(0).get("estado").asString())
                .isEqualTo("ACTIVO");
    }

    @Test
    void noSeReasignaAUnaPantallaDadaDeBaja() throws Exception {
        int idModulo = agregarModulo(idPantalla, "MLD-C1");
        ejecutar(post("/api/activos/{id}/baja", idOtraPantalla), Map.of("motivo", "Estructura dañada"))
                .andExpect(status().isOk());

        ejecutar(post(rutaModulo(idPantalla, idModulo) + "/reasignacion"),
                Map.of("idActivoDestino", idOtraPantalla, "motivo", "Completar la otra pantalla"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void elHistorialDeUnModuloReasignadoMuestraSuRecorrido() throws Exception {
        int idModulo = agregarModulo(idPantalla, "MLD-D1");

        JsonNode reasignado = cuerpo(ejecutar(post(rutaModulo(idPantalla, idModulo) + "/reasignacion"),
                Map.of("idActivoDestino", idOtraPantalla, "motivo", "Completar la pantalla del escenario"))
                .andExpect(status().isOk()));

        assertThat(reasignado.get("idActivoPadre").asInt()).isEqualTo(idOtraPantalla);
        assertThat(leer(get("/api/activos/{id}/modulos", idPantalla))).isEmpty();
        JsonNode historial = leer(get(rutaModulo(idOtraPantalla, idModulo) + "/historial"));
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).get("tipo").asString()).isEqualTo("ALTA");
        assertThat(historial.get(0).get("pantallaDestino").asString()).isEqualTo(codigoPantalla);
        assertThat(historial.get(1).get("tipo").asString()).isEqualTo("REASIGNACION");
        assertThat(historial.get(1).get("pantallaOrigen").asString()).isEqualTo(codigoPantalla);
        assertThat(historial.get(1).get("pantallaDestino").asString()).isEqualTo(codigoOtraPantalla);
        assertThat(historial.get(1).get("registradoPor").asString()).isNotBlank();
    }

    @Test
    void laCargaInicialRegistraElAltaDeLosModulosExistentes() throws Exception {
        jdbc.update("""
                insert into modulos_led (id_activo_padre, codigo_modulo, estado, fecha_creacion)
                values (?, 'MLD-E1', 'DEFECTUOSO', timestamp '2026-03-02 09:00')
                """, idPantalla);
        String migracion = new ClassPathResource(MIGRACION_HISTORIAL).getContentAsString(StandardCharsets.UTF_8);

        jdbc.update(migracion.substring(migracion.indexOf(CARGA_INICIAL)).trim().replaceAll(";$", ""));

        Map<String, Object> alta = jdbc.queryForMap("""
                select h.tipo, h.id_activo_destino, h.estado_nuevo, h.fecha from historial_modulos_led h
                join modulos_led m on m.id_modulo = h.id_modulo where m.codigo_modulo = 'MLD-E1'
                """);
        assertThat(alta.get("tipo")).isEqualTo("ALTA");
        assertThat(alta.get("id_activo_destino")).isEqualTo(idPantalla);
        assertThat(alta.get("estado_nuevo")).isEqualTo("DEFECTUOSO");
        assertThat(alta.get("fecha").toString()).startsWith("2026-03-02 09:00");
    }

    @Test
    void unTecnicoNoReemplazaNiReasignaModulos() throws Exception {
        int idModulo = agregarModulo(idPantalla, "MLD-F1");
        crear("/api/usuarios", Map.of("nombreUsuario", "tecnico.modulos", "nombres", "Técnico",
                "apellidoPaterno", "Módulos", "email", "tecnico.modulos@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasena(iniciarSesion("tecnico.modulos", CONTRASENA_TEMPORAL));

        mvc.perform(autorizado(post(rutaModulo(idPantalla, idModulo) + "/reemplazo"), tokenTecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("codigoNuevo", "MLD-F2", "motivo", "Falla"))))
                .andExpect(status().isForbidden());
        mvc.perform(autorizado(get(rutaModulo(idPantalla, idModulo) + "/historial"), tokenTecnico))
                .andExpect(status().isOk());
    }

    private int agregarModulo(int idActivo, String codigo) throws Exception {
        return crear("/api/activos/" + idActivo + "/modulos", Map.of("codigo", codigo));
    }

    private static String rutaModulo(int idActivo, int idModulo) {
        return "/api/activos/" + idActivo + "/modulos/" + idModulo;
    }

    private int crear(String ruta, Map<String, ?> datos) throws Exception {
        return cuerpo(ejecutar(post(ruta), datos).andExpect(status().isCreated())).get("id").asInt();
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> datos) throws Exception {
        return mvc.perform(autorizado(solicitud, token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos)));
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud) throws Exception {
        return cuerpo(mvc.perform(autorizado(solicitud, token)).andExpect(status().isOk()));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud,
                                                           String tokenUsuario) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUsuario);
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
