package com.visual.backend.finanzas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import jakarta.persistence.EntityManager;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Registro basico de proveedores contra PostgreSQL (base de pruebas); cada prueba se revierte. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoProveedoresIntegracionTest {

    private static final String RUTA = "/api/finanzas/proveedores";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private EntityManager entityManager;

    private String token;

    @BeforeEach
    void iniciarSesion() throws Exception {
        token = json.readTree(mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", "admin", "contrasena", "admin123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("tokenAcceso").asString();
    }

    @Test
    void registraBuscaYEliminaProveedoresConNitUnico() throws Exception {
        JsonNode creado = cuerpo(ejecutar(post(RUTA), proveedor("Transportes de prueba S.R.L.", "990077001"))
                .andExpect(status().isCreated()));
        int id = creado.get("id").asInt();
        assertThat(creado.get("servicios")).hasSize(2);

        ejecutar(post(RUTA), proveedor("Otro proveedor de prueba", "990077001")).andExpect(status().isConflict());
        ejecutar(post(RUTA), proveedor("NIT inválido", "12AB")).andExpect(status().isBadRequest());

        JsonNode porServicio = cuerpo(mvc.perform(autorizado(get(RUTA).param("texto", "grúa de prueba")))
                .andExpect(status().isOk()));
        assertThat(porServicio.get("contenido")).singleElement()
                .satisfies(fila -> assertThat(fila.get("id").asInt()).isEqualTo(id));
        nuevaSolicitud();

        ejecutar(put(RUTA + "/" + id), proveedor("Transportes de prueba S.R.L.", "")).andExpect(status().isOk());
        nuevaSolicitud();
        assertThat(cuerpo(mvc.perform(autorizado(get(RUTA + "/" + id))).andExpect(status().isOk()))
                .get("nit").isNull()).isTrue();

        mvc.perform(autorizado(delete(RUTA + "/" + id))).andExpect(status().isNoContent());
        nuevaSolicitud();
        mvc.perform(autorizado(get(RUTA + "/" + id))).andExpect(status().isNotFound());
    }

    /** Cada peticion real usa su propio contexto de persistencia; la prueba comparte uno. */
    private void nuevaSolicitud() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Map<String, Object> proveedor(String razonSocial, String nit) {
        return Map.of("razonSocial", razonSocial, "nit", nit, "telefono", "22445566",
                "servicios", List.of("Transporte de carga", "Grúa de prueba"));
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> datos) throws Exception {
        return mvc.perform(autorizado(solicitud).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos)));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
