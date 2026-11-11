package com.visual.backend.activos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujo del modulo de activos contra PostgreSQL (base de pruebas). Cada
 * prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoActivosIntegracionTest {

    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String CONTRASENA_DEFINITIVA = "definitiva123";
    private static final String PREFIJO_PRUEBA = "PRB";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    private String tokenAdministrador;
    private int idBodegaPrincipal;
    private int idBodegaSecundaria;
    private int idCategoria;
    private int idFabricante;

    @BeforeEach
    void prepararCatalogos() throws Exception {
        tokenAdministrador = iniciarSesion("admin", "admin123456");
        JsonNode bodegas = leer(get("/api/activos/catalogos/bodegas"), tokenAdministrador);
        idBodegaPrincipal = bodegas.get(0).get("id").asInt();
        idBodegaSecundaria = bodegas.get(1).get("id").asInt();
        idCategoria = leer(get("/api/activos/catalogos/categorias"), tokenAdministrador).get(0).get("id").asInt();
        idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante Prueba"));
    }

    @Test
    void lasHorasDeUsoGeneranAlertaYElMantenimientoPreventivoLaReiniciaYRegistraSuCosto() throws Exception {
        int idModelo = crearModelo("Pantalla prueba", "UNIDAD", 10);
        int idActivo = registrarActivos(Map.of("codigoInventario", "prb-0001", "idModelo", idModelo,
                "idBodega", idBodegaPrincipal, "cantidad", 1)).get(0).get("id").asInt();
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(3);

        registrarUso(idActivo, base, base.plusMinutes(570), tokenAdministrador).andExpect(status().isCreated());
        JsonNode activo = leer(get("/api/activos/{id}", idActivo), tokenAdministrador);
        assertThat(activo.get("codigoInventario").asString()).isEqualTo("PRB-0001");
        assertThat(activo.get("horasDesdeMantenimiento").decimalValue()).isEqualByComparingTo("9.50");
        assertThat(activo.get("nivelAlerta").asString()).isEqualTo("PROXIMO");
        assertThat(leer(get("/api/activos").param("soloAlertas", "true"), tokenAdministrador)
                .get("totalElementos").asInt()).isEqualTo(1);

        registrarUso(idActivo, base.plusHours(1), base.plusHours(2), tokenAdministrador)
                .andExpect(status().isUnprocessableContent());

        int idMantenimiento = crear("/api/activos/" + idActivo + "/mantenimientos",
                Map.of("idTipoMantenimiento", idTipoPreventivo(), "fechaInicio", base.plusDays(1).toString()));
        assertThat(leer(get("/api/activos/{id}", idActivo), tokenAdministrador).get("estado").asString())
                .isEqualTo("EN_MANTENIMIENTO");
        registrarUso(idActivo, base.plusDays(1).plusHours(2), base.plusDays(1).plusHours(3), tokenAdministrador)
                .andExpect(status().isUnprocessableContent());

        ejecutar(post("/api/activos/{id}/mantenimientos/{idM}/finalizacion", idActivo, idMantenimiento),
                Map.of("fechaFin", base.plusDays(2).toString(), "condicionFinal", "OPERATIVO", "costo", 350.5),
                tokenAdministrador).andExpect(status().isOk());
        JsonNode egreso = leer(get("/api/finanzas/movimientos").param("texto", "PRB-0001"), tokenAdministrador)
                .get("pagina").get("contenido").get(0);
        assertThat(egreso.get("concepto").asString()).isEqualTo("Mantenimiento preventivo · PRB-0001");
        assertThat(egreso.get("monto").decimalValue()).isEqualByComparingTo("350.50");
        assertThat(egreso.get("fecha").asString()).isEqualTo(base.plusDays(2).toLocalDate().toString());
        assertThat(egreso.get("idMantenimiento").asInt()).isEqualTo(idMantenimiento);
        assertThat(egreso.get("proyecto").isNull()).isTrue();
        JsonNode reparado = leer(get("/api/activos/{id}", idActivo), tokenAdministrador);
        assertThat(reparado.get("estado").asString()).isEqualTo("DISPONIBLE");
        assertThat(reparado.get("horasTotales").decimalValue()).isEqualByComparingTo("9.50");
        assertThat(reparado.get("horasDesdeMantenimiento").decimalValue()).isZero();
        assertThat(reparado.get("nivelAlerta").asString()).isEqualTo("AL_DIA");

        mvc.perform(autorizado(delete("/api/activos/{id}", idActivo), tokenAdministrador))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void seRegistranVariasUnidadesConCodigosCorrelativosDelModelo() throws Exception {
        int idModelo = crearModelo("Pantalla lote", "UNIDAD", null);

        JsonNode primeras = registrarActivos(Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal,
                "cantidad", 3, "numerosSerie", List.of("S-1", "S-2", "S-3")));
        JsonNode siguientes = registrarActivos(Map.of("idModelo", idModelo, "idBodega", idBodegaSecundaria,
                "cantidad", 2));

        assertThat(primeras).extracting(activo -> activo.get("codigoInventario").asString())
                .containsExactly("PRB-0001", "PRB-0002", "PRB-0003");
        assertThat(primeras.get(2).get("numeroSerie").asString()).isEqualTo("S-3");
        assertThat(siguientes).extracting(activo -> activo.get("codigoInventario").asString())
                .containsExactly("PRB-0004", "PRB-0005");
        assertThat(leer(get("/api/activos/modelos/{id}", idModelo), tokenAdministrador)
                .get("cantidadInventario").asInt()).isEqualTo(5);
        assertThat(leer(get("/api/activos").param("idModelo", String.valueOf(idModelo)), tokenAdministrador)
                .get("totalElementos").asInt()).isEqualTo(5);

        crearConEstado("/api/activos", Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal, "cantidad", 2,
                "codigoInventario", "MANUAL-1")).andExpect(status().isUnprocessableContent());
        crearConEstado("/api/activos", Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal, "cantidad", 2,
                "numerosSerie", List.of("S-9"))).andExpect(status().isUnprocessableContent());
        crearConEstado("/api/activos", Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal, "cantidad", 1,
                "numerosSerie", List.of("S-2"))).andExpect(status().isConflict());
    }

    @Test
    void elInventarioPorLoteControlaExistenciasYTraslados() throws Exception {
        int idModelo = crearModelo("Cable prueba", "LOTE", null);

        moverLote(idModelo, idBodegaPrincipal, "INGRESO", 20).andExpect(status().isCreated());
        ejecutar(post("/api/activos/lotes/traslados"), Map.of("idModelo", idModelo,
                "idBodegaOrigen", idBodegaPrincipal, "idBodegaDestino", idBodegaSecundaria, "cantidad", 5),
                tokenAdministrador).andExpect(status().isCreated());
        moverLote(idModelo, idBodegaPrincipal, "SALIDA", 50)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("insuficiente")));

        JsonNode existencias = leer(get("/api/activos/lotes").param("texto", "Cable prueba"), tokenAdministrador);
        assertThat(existencias).hasSize(2);
        assertThat(cantidadEn(existencias, idBodegaPrincipal)).isEqualTo(15);
        assertThat(cantidadEn(existencias, idBodegaSecundaria)).isEqualTo(5);

        crearConEstado("/api/activos", Map.of("codigoInventario", "PRB-LOTE", "idModelo", idModelo,
                "idBodega", idBodegaPrincipal, "cantidad", 1)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void unTecnicoRegistraUsoPeroNoAdministraElInventario() throws Exception {
        int idModelo = crearModelo("Consola prueba", "UNIDAD", null);
        int idActivo = registrarActivos(Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal,
                "cantidad", 1)).get(0).get("id").asInt();
        crear("/api/usuarios", Map.of("nombreUsuario", "tecnico.activos", "nombres", "Tecnico",
                "apellidoPaterno", "Activos", "email", "tecnico.activos@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasenaTemporal(iniciarSesion("tecnico.activos", CONTRASENA_TEMPORAL));
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(1);

        registrarUso(idActivo, base, base.plusHours(4), tokenTecnico).andExpect(status().isCreated());
        mvc.perform(autorizado(get("/api/activos/{id}", idActivo), tokenTecnico)).andExpect(status().isOk());
        ejecutar(post("/api/activos"), Map.of("idModelo", idModelo, "idBodega", idBodegaPrincipal,
                "cantidad", 1), tokenTecnico).andExpect(status().isForbidden());
        moverLote(idModelo, idBodegaPrincipal, "INGRESO", 1, tokenTecnico).andExpect(status().isForbidden());
    }

    private int crearModelo(String nombre, String tipoControl, Integer horasMantenimiento) throws Exception {
        Map<String, Object> modelo = new HashMap<>(Map.of("idFabricante", idFabricante,
                "idCategoria", idCategoria, "nombre", nombre, "tipoControl", tipoControl));
        modelo.put("horasMantenimiento", horasMantenimiento);
        modelo.put("prefijoCodigo", "UNIDAD".equals(tipoControl) ? PREFIJO_PRUEBA : null);
        return crear("/api/activos/modelos", modelo);
    }

    private JsonNode registrarActivos(Map<String, ?> cuerpo) throws Exception {
        String respuesta = crearConEstado("/api/activos", cuerpo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private int idTipoPreventivo() throws Exception {
        for (JsonNode tipo : leer(get("/api/activos/catalogos/tipos-mantenimiento"), tokenAdministrador)) {
            if (tipo.get("reiniciaHoras").asBoolean()) {
                return tipo.get("id").asInt();
            }
        }
        throw new IllegalStateException("No existe un tipo de mantenimiento preventivo");
    }

    private ResultActions registrarUso(int idActivo, LocalDateTime inicio, LocalDateTime fin, String token)
            throws Exception {
        return ejecutar(post("/api/activos/{id}/usos", idActivo),
                Map.of("fechaInicio", inicio.toString(), "fechaFin", fin.toString()), token);
    }

    private ResultActions moverLote(int idModelo, int idBodega, String tipo, int cantidad) throws Exception {
        return moverLote(idModelo, idBodega, tipo, cantidad, tokenAdministrador);
    }

    private ResultActions moverLote(int idModelo, int idBodega, String tipo, int cantidad, String token)
            throws Exception {
        return ejecutar(post("/api/activos/lotes/movimientos"), Map.of("idModelo", idModelo, "idBodega", idBodega,
                "tipo", tipo, "cantidad", cantidad), token);
    }

    private static int cantidadEn(JsonNode existencias, int idBodega) {
        for (JsonNode existencia : existencias) {
            if (existencia.get("idBodega").asInt() == idBodega) {
                return existencia.get("cantidad").asInt();
            }
        }
        throw new AssertionError("Sin existencia en la bodega " + idBodega);
    }

    private int crear(String ruta, Map<String, ?> cuerpo) throws Exception {
        String respuesta = crearConEstado(ruta, cuerpo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asInt();
    }

    private ResultActions crearConEstado(String ruta, Map<String, ?> cuerpo) throws Exception {
        return ejecutar(post(ruta), cuerpo, tokenAdministrador);
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> cuerpo, String token)
            throws Exception {
        return mvc.perform(autorizado(solicitud, token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        String respuesta = mvc.perform(autorizado(solicitud, token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private String iniciarSesion(String nombreUsuario, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", nombreUsuario,
                                "contrasena", contrasena))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("tokenAcceso").asString();
    }

    private String cambiarContrasenaTemporal(String token) throws Exception {
        String respuesta = ejecutar(put("/api/auth/contrasena"), Map.of("contrasenaActual", CONTRASENA_TEMPORAL,
                "nuevaContrasena", CONTRASENA_DEFINITIVA), token)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("tokenAcceso").asString();
    }

    private static MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud, String token) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
