package com.visual.backend.gerencial.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Alertas persistentes contra PostgreSQL: sincronizacion, atencion, conteo de
 * pendientes en los KPIs y alerta de reserva en riesgo. Cada prueba se
 * revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoAlertasIntegracionTest {

    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String ALERTAS = "/api/gerencial/alertas";
    private static final String CONSUMIBLES = "/api/subcontratacion/consumibles";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private EntityManager entityManager;

    private String token;
    private LocalDate hoy;

    @BeforeEach
    void iniciarSesion() throws Exception {
        token = sesion("admin", "admin123456");
        hoy = LocalDate.now();
    }

    @Test
    void laSincronizacionCreaYResuelveLasAlertas() throws Exception {
        int idConsumible = consumibleBajoMinimo("Cinta gaffer de alertas");

        JsonNode pendiente = alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "PENDIENTE");
        assertThat(pendiente).isNotNull();
        assertThat(pendiente.get("detalle").asString()).startsWith("Cinta gaffer de alertas: 0 rollo");

        ejecutar(post(CONSUMIBLES + "/{id}/ingresos", idConsumible), Map.of("cantidad", "10"))
                .andExpect(status().isOk());

        assertThat(alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "PENDIENTE")).isNull();
        JsonNode resuelta = alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "RESUELTA");
        assertThat(resuelta.get("id").asLong()).isEqualTo(pendiente.get("id").asLong());
        assertThat(resuelta.hasNonNull("fechaResolucion")).isTrue();
    }

    @Test
    void unaAlertaAtendidaNoSeReabreYLosKpisCuentanSoloPendientes() throws Exception {
        int idConsumible = consumibleBajoMinimo("Pilas de alertas");
        JsonNode pendiente = alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "PENDIENTE");
        long advertenciasAntes = kpisDeHoy().get("alertas").get("advertencias").asLong();

        JsonNode atendida = cuerpo(ejecutar(post(ALERTAS + "/{id}/atencion", pendiente.get("id").asLong()),
                Map.of("comentario", "Compra solicitada a la ferretería")).andExpect(status().isOk()));

        assertThat(atendida.get("estado").asString()).isEqualTo("ATENDIDA");
        assertThat(atendida.get("nombreAtendio").asString()).isNotBlank();
        assertThat(alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "PENDIENTE")).isNull();
        assertThat(alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "ATENDIDA").get("id").asLong())
                .isEqualTo(pendiente.get("id").asLong());
        assertThat(kpisDeHoy().get("alertas").get("advertencias").asLong()).isEqualTo(advertenciasAntes - 1);
    }

    @Test
    void atenderUnaAlertaResueltaSeRechaza() throws Exception {
        int idConsumible = consumibleBajoMinimo("Precintos de alertas");
        long idAlerta = alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "PENDIENTE").get("id").asLong();
        ejecutar(post(CONSUMIBLES + "/{id}/ingresos", idConsumible), Map.of("cantidad", "20"))
                .andExpect(status().isOk());
        alertaDe("STOCK_BAJO", String.valueOf(idConsumible), "RESUELTA");

        ejecutar(post(ALERTAS + "/{id}/atencion", idAlerta), Map.of("comentario", "Tarde"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void unEquipoReservadoQueEntraEnMantenimientoGeneraUnaAlertaCritica() throws Exception {
        int[] proyectoYEvento = proyectoConEvento();
        int idActivo = equipo();
        JsonNode reserva = cuerpo(ejecutar(post("/api/logistica/eventos/{id}/reservas", proyectoYEvento[1]),
                Map.of("idsActivos", List.of(idActivo))).andExpect(status().isCreated())).get(0);
        int idPreventivo = 0;
        for (JsonNode tipo : leer(get("/api/activos/catalogos/tipos-mantenimiento"))) {
            if (tipo.get("reiniciaHoras").asBoolean()) {
                idPreventivo = tipo.get("id").asInt();
            }
        }
        ejecutar(post("/api/activos/{id}/mantenimientos", idActivo), Map.of("idTipoMantenimiento", idPreventivo,
                "fechaInicio", hoy.atStartOfDay().toString())).andExpect(status().isCreated());
        nuevaSolicitud();

        JsonNode alerta = alertaDe("RESERVA_EN_RIESGO", reserva.get("id").asString(), "PENDIENTE");

        assertThat(alerta).isNotNull();
        assertThat(alerta.get("severidad").asString()).isEqualTo("CRITICA");
        assertThat(alerta.get("idActivo").asInt()).isEqualTo(idActivo);
        assertThat(alerta.get("idEvento").asInt()).isEqualTo(proyectoYEvento[1]);
    }

    @Test
    void sinPermisoDeReportesNoSeAtiendenAlertas() throws Exception {
        crear("/api/usuarios", Map.of("nombreUsuario", "tecnico.alertas", "nombres", "Técnico",
                "apellidoPaterno", "Alertas", "email", "tecnico.alertas@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasena(sesion("tecnico.alertas", CONTRASENA_TEMPORAL));

        mvc.perform(autorizado(post(ALERTAS + "/{id}/atencion", 1), tokenTecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("comentario", "Revisado"))))
                .andExpect(status().isForbidden());
        mvc.perform(autorizado(get(ALERTAS), tokenTecnico)).andExpect(status().isForbidden());
    }

    /** Cada peticion real usa su propio contexto de persistencia; la prueba comparte uno. */
    private void nuevaSolicitud() {
        entityManager.flush();
        entityManager.clear();
    }

    private int consumibleBajoMinimo(String nombre) throws Exception {
        return crear(CONSUMIBLES, Map.of("nombre", nombre, "unidadMedida", "rollo", "stockMinimo", "5",
                "costoUnitario", "10", "activo", true));
    }

    private JsonNode alertaDe(String tipo, String clave, String estado) throws Exception {
        for (JsonNode alerta : leer(get(ALERTAS).param("estado", estado).param("tipo", tipo))) {
            if (alerta.get("clave").asString().equals(clave)) {
                return alerta;
            }
        }
        return null;
    }

    private JsonNode kpisDeHoy() throws Exception {
        return leer(get("/api/gerencial/kpis").param("desde", hoy.toString()).param("hasta", hoy.toString()));
    }

    private int equipo() throws Exception {
        int idBodega = leer(get("/api/activos/catalogos/bodegas")).get(0).get("id").asInt();
        int idCategoria = leer(get("/api/activos/catalogos/categorias")).get(0).get("id").asInt();
        int idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante alertas"));
        int idModelo = crear("/api/activos/modelos", Map.of("idFabricante", idFabricante, "idCategoria",
                idCategoria, "nombre", "Pantalla alertas", "tipoControl", "UNIDAD", "prefijoCodigo", "ALR"));
        return cuerpo(ejecutar(post("/api/activos"), Map.of("idModelo", idModelo, "idBodega", idBodega,
                "cantidad", 1)).andExpect(status().isCreated())).get(0).get("id").asInt();
    }

    private int[] proyectoConEvento() throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Pantalla de prueba alertas",
                "unidadMedida", "jornada", "precioReferencial", 5000, "activo", true));
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        int idCliente = crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente de prueba alertas", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "770011088"))));
        String dia = hoy.plusDays(15).toString();
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Gala de prueba alertas");
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", hoy.plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", dia);
        cotizacion.put("fechaEventoFin", dia);
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 5000,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        int proyecto = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion), token)).andExpect(status().isOk())).get("idProyecto").asInt();
        int idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Salón alertas de prueba",
                "direccion", "Av. Ballivián 1200", "ciudad", "La Paz"));
        int evento = crear("/api/comercial/proyectos/" + proyecto + "/eventos", Map.of("idLugar", idLugar,
                "nombre", "Gala con pantalla", "fechaInicio", dia + "T19:00:00", "fechaFin", dia + "T23:00:00"));
        return new int[] {proyecto, evento};
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
