package com.visual.backend.comercial.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/** Resumen comercial de un cliente con cotizaciones en varios estados, contra PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoResumenClienteIntegracionTest {

    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Test
    void resumeLasCotizacionesYProyectosDelCliente() throws Exception {
        String token = sesion("admin", "admin123456");
        int idServicio = crear(token, "/api/comercial/catalogos/servicios", Map.of("nombre", "Servicio resumen",
                "unidadMedida", "jornada", "precioReferencial", 1000, "activo", true));
        int idTipoCliente = idPorNombre(leer(token, get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(token, get("/api/comercial/catalogos/tipos-documento")), "NIT");
        int idCliente = crear(token, "/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente de prueba resumen", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "770033001"))));
        int aceptada = cotizacion(token, idCliente, idServicio, "4000");
        int rechazada = cotizacion(token, idCliente, idServicio, "1500");
        cotizacion(token, idCliente, idServicio, "2500");
        mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion", aceptada), token))
                .andExpect(status().isOk());
        mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/rechazo", rechazada), token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("motivo", "Presupuesto insuficiente")))).andExpect(status().isOk());

        JsonNode resumen = leer(token, get("/api/comercial/clientes/{id}/resumen", idCliente));

        assertThat(resumen.get("cotizacionesPorEstado").get("ACEPTADA").asLong()).isEqualTo(1);
        assertThat(resumen.get("cotizacionesPorEstado").get("RECHAZADA").asLong()).isEqualTo(1);
        assertThat(resumen.get("cotizacionesPorEstado").get("BORRADOR").asLong()).isEqualTo(1);
        assertThat(resumen.get("importeCotizado").decimalValue()).isEqualByComparingTo("8000");
        assertThat(resumen.get("importeContratado").decimalValue()).isEqualByComparingTo("4000");
        assertThat(resumen.get("proyectosPorEstado").get("PLANIFICACION").asLong()).isEqualTo(1);
        assertThat(resumen.get("ultimasCotizaciones")).hasSize(3);
        assertThat(resumen.get("ultimosProyectos")).singleElement()
                .satisfies(proyecto -> assertThat(proyecto.get("estado").asString()).isEqualTo("PLANIFICACION"));
        mvc.perform(autorizado(get("/api/comercial/clientes/{id}/resumen", 999_999), token))
                .andExpect(status().isNotFound());

        crear(token, "/api/usuarios", Map.of("nombreUsuario", "tecnico.resumen", "nombres", "Técnico",
                "apellidoPaterno", "Resumen", "email", "tecnico.resumen@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasena(sesion("tecnico.resumen", CONTRASENA_TEMPORAL));
        mvc.perform(autorizado(get("/api/comercial/clientes/{id}/resumen", idCliente), tokenTecnico))
                .andExpect(status().isForbidden());
    }

    private int cotizacion(String token, int idCliente, int idServicio, String precio) throws Exception {
        LocalDate hoy = LocalDate.now();
        Map<String, Object> datos = new HashMap<>();
        datos.put("idCliente", idCliente);
        datos.put("titulo", "Cotización de resumen");
        datos.put("fechaEmision", hoy.toString());
        datos.put("fechaValidez", hoy.plusDays(5).toString());
        datos.put("fechaEventoInicio", hoy.plusDays(20).toString());
        datos.put("fechaEventoFin", hoy.plusDays(20).toString());
        datos.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", precio,
                "descuento", 0)));
        return crear(token, "/api/comercial/cotizaciones", datos);
    }

    private static int idPorNombre(JsonNode elementos, String nombre) {
        for (JsonNode elemento : elementos) {
            if (elemento.get("nombre").asString().equals(nombre)) {
                return elemento.get("id").asInt();
            }
        }
        throw new IllegalStateException("No existe " + nombre);
    }

    private int crear(String token, String ruta, Map<String, ?> datos) throws Exception {
        return cuerpo(mvc.perform(autorizado(post(ruta), token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos))).andExpect(status().isCreated())).get("id").asInt();
    }

    private JsonNode leer(String token, MockHttpServletRequestBuilder solicitud) throws Exception {
        return cuerpo(mvc.perform(autorizado(solicitud, token)).andExpect(status().isOk()));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud, String token) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String sesion(String usuario, String contrasena) throws Exception {
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
