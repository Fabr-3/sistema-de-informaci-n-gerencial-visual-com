package com.visual.backend.accesos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.dominio.excepcion.AutenticacionExcepcion;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujo completo contra PostgreSQL (base proyecto_visual_com_test). Cada
 * prueba se revierte al terminar, salvo el administrador inicial que crea la
 * aplicacion al arrancar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoAccesosIntegracionTest {

    private static final String COOKIE_REFRESCO = "visual_refresco";
    private static final int ID_ROL_TECNICO = 3;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Test
    void sinTokenLasRutasProtegidasRespondenNoAutorizado() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void inicioDeSesionEntregaTokenYCookieHttpOnly() throws Exception {
        mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
                .andExpect(jsonPath("$.tokenRefresco").doesNotExist())
                .andExpect(jsonPath("$.usuario.codigoRol").value("ADMINISTRADOR"))
                .andExpect(cookie().httpOnly(COOKIE_REFRESCO, true))
                .andExpect(cookie().path(COOKIE_REFRESCO, "/api/auth"));
    }

    @Test
    void credencialesInvalidasRespondenNoAutorizado() throws Exception {
        mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "incorrecta1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(AutenticacionExcepcion.CREDENCIALES_INVALIDAS));
    }

    @Test
    void usuarioConContrasenaTemporalSoloPuedeCambiarla() throws Exception {
        String tokenAdministrador = iniciarSesion("admin", "admin123456").get("tokenAcceso").asString();
        mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"tecnico.prueba","nombres":"Tecnico","apellidoPaterno":"Prueba",
                                 "email":"tecnico.prueba@visual.com.bo","idRol":%d,"contrasenaTemporal":"temporal123"}
                                """.formatted(ID_ROL_TECNICO)))
                .andExpect(status().isCreated());

        JsonNode sesionTemporal = iniciarSesion("tecnico.prueba", "temporal123");
        assertThat(sesionTemporal.get("usuario").get("debeCambiarContrasena").asBoolean()).isTrue();
        String tokenTemporal = sesionTemporal.get("tokenAcceso").asString();
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenTemporal)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/auth/contrasena").header(HttpHeaders.AUTHORIZATION, bearer(tokenTemporal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"temporal123\",\"nuevaContrasena\":\"definitiva123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.debeCambiarContrasena").value(false))
                .andExpect(jsonPath("$.usuario.permisos").isNotEmpty());
    }

    @Test
    void unTecnicoNoPuedeGestionarUsuarios() throws Exception {
        String tokenAdministrador = iniciarSesion("admin", "admin123456").get("tokenAcceso").asString();
        MvcResult creado = mvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"tecnico.dos","nombres":"Tecnico","apellidoPaterno":"Dos",
                                 "email":"tecnico.dos@visual.com.bo","idRol":%d,"contrasenaTemporal":"temporal123"}
                                """.formatted(ID_ROL_TECNICO)))
                .andExpect(status().isCreated())
                .andReturn();
        int idTecnico = json.readTree(creado.getResponse().getContentAsString()).get("id").asInt();
        String tokenTecnico = cambiarContrasena(iniciarSesion("tecnico.dos", "temporal123"));

        mvc.perform(patch("/api/usuarios/{id}/estado", idTecnico)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTecnico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").header(HttpHeaders.AUTHORIZATION, bearer(tokenTecnico)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elRefrescoRotaLaCookieYElTokenAnteriorQuedaInvalido() throws Exception {
        MvcResult inicio = mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "admin123456")))
                .andExpect(status().isOk())
                .andReturn();
        Cookie original = inicio.getResponse().getCookie(COOKIE_REFRESCO);

        MvcResult refresco = mvc.perform(post("/api/auth/refresco").cookie(original))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotada = refresco.getResponse().getCookie(COOKIE_REFRESCO);
        assertThat(rotada.getValue()).isNotEqualTo(original.getValue());

        mvc.perform(post("/api/auth/refresco").cookie(original)).andExpect(status().isUnauthorized());
        // La reutilizacion revoca tambien la sesion rotada
        mvc.perform(post("/api/auth/refresco").cookie(rotada)).andExpect(status().isUnauthorized());
    }

    @Test
    void elAdministradorConsultaLaAuditoriaDeAccesos() throws Exception {
        String token = iniciarSesion("admin", "admin123456").get("tokenAcceso").asString();

        mvc.perform(get("/api/auditoria").param("accion", "INICIO_SESION")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].accion").value("INICIO_SESION"))
                .andExpect(jsonPath("$.contenido[0].nombreUsuario").value("admin"));
    }

    private JsonNode iniciarSesion(String nombreUsuario, String contrasena) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(nombreUsuario, contrasena)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    /** Cambia la contrasena temporal y devuelve el nuevo token de acceso. */
    private String cambiarContrasena(JsonNode sesion) throws Exception {
        MvcResult resultado = mvc.perform(put("/api/auth/contrasena")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sesion.get("tokenAcceso").asString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"temporal123\",\"nuevaContrasena\":\"definitiva123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("tokenAcceso").asString();
    }

    private String credenciales(String nombreUsuario, String contrasena) {
        return json.writeValueAsString(java.util.Map.of("nombreUsuario", nombreUsuario, "contrasena", contrasena));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
