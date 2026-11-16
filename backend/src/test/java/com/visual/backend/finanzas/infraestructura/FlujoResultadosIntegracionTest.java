package com.visual.backend.finanzas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resultados por proyecto con filtro por cliente y periodo y orden por
 * margen, contra PostgreSQL. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoResultadosIntegracionTest {

    private static final String RESULTADOS = "/api/finanzas/resultados/proyectos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    private String token;
    private LocalDate hoy;
    private Map<String, Integer> tipos;
    private int idCliente;
    private int idServicio;
    private int idRentable;
    private int idGrande;
    private int idConPerdida;

    @BeforeEach
    void preparar() throws Exception {
        token = cuerpo(mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", "admin", "contrasena", "admin123456"))))
                .andExpect(status().isOk())).get("tokenAcceso").asString();
        hoy = LocalDate.now();
        tipos = new HashMap<>();
        for (JsonNode tipo : leer(get("/api/finanzas/tipos-movimiento"))) {
            tipos.put(tipo.get("nombre").asString(), tipo.get("id").asInt());
        }
        idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Servicio de prueba resultados",
                "unidadMedida", "jornada", "precioReferencial", 1000, "activo", true));
        idCliente = cliente("Cliente de prueba resultados", "770022001");
        idRentable = proyecto(idCliente, "Rentable", hoy.plusDays(5), "1000", "200");
        idGrande = proyecto(idCliente, "Grande", hoy.plusDays(10), "5000", "4000");
        idConPerdida = proyecto(idCliente, "Con pérdida", hoy.plusDays(60), "300", "400");
        proyecto(cliente("Otro cliente de prueba resultados", "770022002"), "Ajeno", hoy.plusDays(5), "9000", "0");
    }

    @Test
    void ordenaPorMargenYFiltraPorCliente() throws Exception {
        assertThat(ids("MARGEN_DESC")).containsExactly(idGrande, idRentable, idConPerdida);
        assertThat(ids("MARGEN_ASC")).containsExactly(idConPerdida, idRentable, idGrande);
        assertThat(ids("MARGEN_PORCENTAJE_DESC")).containsExactly(idRentable, idGrande, idConPerdida);
        assertThat(ids("FECHA_DESC")).containsExactly(idConPerdida, idGrande, idRentable);
    }

    @Test
    void filtraLosProyectosQueSeSuperponenConElPeriodo() throws Exception {
        JsonNode pagina = leer(get(RESULTADOS).param("idCliente", String.valueOf(idCliente))
                .param("desde", hoy.toString()).param("hasta", hoy.plusDays(30).toString()));

        assertThat(pagina.get("contenido").valueStream().map(fila -> fila.get("idProyecto").asInt()))
                .containsExactlyInAnyOrder(idRentable, idGrande);
        mvc.perform(autorizado(get(RESULTADOS).param("desde", hoy.toString())
                .param("hasta", hoy.minusDays(1).toString()))).andExpect(status().isUnprocessableContent());
        mvc.perform(autorizado(get(RESULTADOS).param("orden", "costos; drop table proyectos")))
                .andExpect(status().isBadRequest());
    }

    private List<Integer> ids(String orden) throws Exception {
        return leer(get(RESULTADOS).param("idCliente", String.valueOf(idCliente)).param("orden", orden))
                .get("contenido").valueStream().map(fila -> fila.get("idProyecto").asInt()).toList();
    }

    private int cliente(String razonSocial, String nit) throws Exception {
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        return crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente, "razonSocial", razonSocial,
                "documentos", List.of(Map.of("idTipoDocumento", idNit, "numero", nit))));
    }

    private int proyecto(int cliente, String titulo, LocalDate dia, String ingreso, String egreso) throws Exception {
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", cliente);
        cotizacion.put("titulo", titulo);
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", hoy.plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", dia.toString());
        cotizacion.put("fechaEventoFin", dia.toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 1000,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        int idProyecto = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion))).andExpect(status().isOk())).get("idProyecto").asInt();
        movimiento("Cobro de servicios", ingreso, idProyecto);
        if (!"0".equals(egreso)) {
            movimiento("Transporte y viáticos", egreso, idProyecto);
        }
        return idProyecto;
    }

    private void movimiento(String tipo, String monto, int idProyecto) throws Exception {
        crear("/api/finanzas/movimientos", Map.of("idTipo", tipos.get(tipo), "concepto", tipo, "monto", monto,
                "fecha", hoy.toString(), "idProyecto", idProyecto));
    }

    private static int idPorNombre(JsonNode elementos, String nombre) {
        for (JsonNode elemento : elementos) {
            if (elemento.get("nombre").asString().equals(nombre)) {
                return elemento.get("id").asInt();
            }
        }
        throw new IllegalStateException("No existe " + nombre);
    }

    private int crear(String ruta, Map<String, ?> datos) throws Exception {
        return cuerpo(mvc.perform(autorizado(post(ruta)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos))).andExpect(status().isCreated())).get("id").asInt();
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud) throws Exception {
        return cuerpo(mvc.perform(autorizado(solicitud)).andExpect(status().isOk()));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autorizado(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
