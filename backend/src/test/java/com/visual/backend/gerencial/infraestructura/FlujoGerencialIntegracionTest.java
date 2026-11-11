package com.visual.backend.gerencial.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Informacion gerencial contra PostgreSQL (base de pruebas): KPIs a partir de
 * movimientos, cliente recurrente, alertas de activos y reportes en PDF.
 * Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoGerencialIntegracionTest {

    private static final String KPIS = "/api/gerencial/kpis";
    private static final String CLIENTE = "Cliente gerencial de prueba";
    private static final String FIRMA_PDF = "%PDF";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private LocalDate hoy;

    @BeforeEach
    void preparar() throws Exception {
        token = json.readTree(mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", "admin", "contrasena", "admin123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("tokenAcceso").asString();
        hoy = LocalDate.now();
    }

    @Test
    void losKpisSeCalculanConLosMovimientosYReconocenAlClienteRecurrente() throws Exception {
        JsonNode antes = kpisDeHoy().get("actual");
        int idCliente = cliente();
        int idProyecto = proyectoAceptado(idCliente, "Congreso de prueba");
        proyectoAceptado(idCliente, "Cena de prueba");
        registrar("Cobro de servicios", "Anticipo del congreso", "10000", idProyecto);
        registrar("Transporte y viáticos", "Flete del congreso", "4000", idProyecto);
        registrar("Gastos generales", "Alquiler de oficina", "1500", null);
        nuevaSolicitud();

        JsonNode kpis = kpisDeHoy();
        JsonNode actual = kpis.get("actual");
        assertThat(diferencia(actual, antes, "ingresos")).isEqualByComparingTo("10000");
        assertThat(diferencia(actual, antes, "costosDirectos")).isEqualByComparingTo("4000");
        assertThat(diferencia(actual, antes, "gastosGenerales")).isEqualByComparingTo("1500");
        assertThat(diferencia(actual, antes, "ganancia")).isEqualByComparingTo("4500");
        assertThat(kpis.get("anterior").get("desde").asString()).isEqualTo(hoy.minusDays(1).toString());

        JsonNode recurrente = null;
        for (JsonNode cliente : kpis.get("rentabilidadRecurrente").get("clientes")) {
            if (cliente.get("idCliente").asInt() == idCliente) {
                recurrente = cliente;
            }
        }
        assertThat(recurrente).isNotNull();
        assertThat(recurrente.get("proyectosContratados").asInt()).isEqualTo(2);
        assertThat(recurrente.get("margen").decimalValue()).isEqualByComparingTo("6000");
        assertThat(recurrente.get("porcentaje").decimalValue()).isEqualByComparingTo("60.0");
        assertThat(kpis.get("utilizacion").get("equipos").has("porcentaje")).isTrue();

        mvc.perform(autorizado(get(KPIS).param("desde", hoy.toString()).param("hasta", hoy.minusDays(1).toString())))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get(KPIS)).andExpect(status().isUnauthorized());
    }

    @Test
    void unEquipoQueSuperaSuIntervaloGeneraUnaAlertaCriticaYSuUsoCuentaEnLaUtilizacion() throws Exception {
        int idActivo = equipoConUso(11);
        nuevaSolicitud();

        JsonNode alerta = null;
        for (JsonNode candidata : leer(get("/api/gerencial/alertas-activos"))) {
            if (candidata.get("idActivo").asInt() == idActivo) {
                alerta = candidata;
            }
        }
        assertThat(alerta).isNotNull();
        assertThat(alerta.get("tipo").asString()).isEqualTo("MANTENIMIENTO_VENCIDO");
        assertThat(alerta.get("severidad").asString()).isEqualTo("CRITICA");
        assertThat(alerta.get("detalle").asString()).startsWith("11,0 h de uso");

        JsonNode categoria = null;
        for (JsonNode candidata : kpisDeHoy().get("utilizacion").get("porCategoria")) {
            if (candidata.get("categoria").asString().equals("Categoría gerencial de prueba")) {
                categoria = candidata.get("tasa");
            }
        }
        assertThat(categoria).isNotNull();
        assertThat(categoria.get("recursos").asInt()).isEqualTo(1);
        assertThat(categoria.get("diasDisponibles").asLong()).isEqualTo(1);
        assertThat(categoria.get("diasOcupados").asLong()).isEqualTo(1);
        assertThat(categoria.get("porcentaje").decimalValue()).isEqualByComparingTo("100.0");
    }

    @Test
    void generaCadaReporteEnPdfYAuditaSuEmision() throws Exception {
        equipoConUso(2);
        nuevaSolicitud();
        for (String tipo : List.of("RESUMEN_GERENCIAL", "EVENTOS", "ESTADO_ACTIVOS", "MANTENIMIENTOS", "PERSONAL")) {
            MvcResult resultado = mvc.perform(autorizado(get("/api/gerencial/reportes/{tipo}", tipo)
                    .param("desde", hoy.withDayOfMonth(1).toString()).param("hasta", hoy.toString())))
                    .andExpect(status().isOk()).andReturn();
            assertThat(resultado.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
            assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(".pdf");
            assertThat(new String(resultado.getResponse().getContentAsByteArray(), 0, FIRMA_PDF.length()))
                    .isEqualTo(FIRMA_PDF);
        }
        mvc.perform(autorizado(get("/api/gerencial/reportes/{tipo}", "DESCONOCIDO")))
                .andExpect(status().isBadRequest());
        nuevaSolicitud();
        assertThat(jdbc.queryForObject("select count(*) from auditoria where accion = 'REPORTE_GENERADO' "
                + "and entidad = 'reportes'", Integer.class)).isGreaterThanOrEqualTo(5);
    }

    /** Equipo de una categoria propia, adquirido hoy, con horas de uso hoy sobre un intervalo de 10 h. */
    private int equipoConUso(int horas) {
        int idCategoria = insertar("insert into categorias_activo (nombre) values ('Categoría gerencial de prueba') "
                + "returning id_categoria");
        int idFabricante = insertar("insert into fabricantes (nombre) values ('Fabricante gerencial') "
                + "returning id_fabricante");
        int idModelo = jdbc.queryForObject("insert into modelos_activo (id_fabricante, id_categoria, nombre, "
                + "tipo_control, horas_mantenimiento) values (?, ?, 'Equipo de prueba', 'UNIDAD', 10) "
                + "returning id_modelo", Integer.class, idFabricante, idCategoria);
        int idBodega = insertar("select min(id_bodega) from bodegas");
        int idActivo = jdbc.queryForObject("insert into activos (codigo_inventario, id_modelo, id_bodega, "
                + "fecha_adquisicion) values ('GER-0001', ?, ?, ?) returning id_activo", Integer.class, idModelo,
                idBodega, hoy);
        LocalDateTime inicio = hoy.atStartOfDay().truncatedTo(ChronoUnit.HOURS);
        jdbc.update("insert into registros_uso (id_activo, fecha_inicio, fecha_fin) values (?, ?, ?)", idActivo,
                inicio, inicio.plusHours(horas));
        return idActivo;
    }

    private int insertar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private JsonNode kpisDeHoy() throws Exception {
        return leer(get(KPIS).param("desde", hoy.toString()).param("hasta", hoy.toString()));
    }

    private static BigDecimal diferencia(JsonNode actual, JsonNode anterior, String campo) {
        return actual.get(campo).decimalValue().subtract(anterior.get(campo).decimalValue());
    }

    private int cliente() throws Exception {
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        return crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente, "razonSocial", CLIENTE,
                "documentos", List.of(Map.of("idTipoDocumento", idNit, "numero", "880011004"))));
    }

    private int proyectoAceptado(int idCliente, String titulo) throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Servicio " + titulo,
                "unidadMedida", "jornada", "precioReferencial", 5000, "activo", true));
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", titulo);
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", hoy.plusDays(5).toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 12000,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        return cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion", idCotizacion)))
                .andExpect(status().isOk())).get("idProyecto").asInt();
    }

    private void registrar(String tipo, String concepto, String monto, Integer idProyecto) throws Exception {
        Map<String, Object> movimiento = new HashMap<>();
        movimiento.put("idTipo", idPorNombre(leer(get("/api/finanzas/tipos-movimiento")), tipo));
        movimiento.put("concepto", concepto);
        movimiento.put("monto", monto);
        movimiento.put("fecha", hoy.toString());
        movimiento.put("idProyecto", idProyecto);
        crear("/api/finanzas/movimientos", movimiento);
    }

    /** Cada peticion real usa su propio contexto de persistencia; la prueba comparte uno. */
    private void nuevaSolicitud() {
        entityManager.flush();
        entityManager.clear();
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
