package com.visual.backend.finanzas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
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
 * Movimientos financieros contra PostgreSQL (base de pruebas): imputacion a
 * proyectos y eventos, proveedor obligatorio y ventas que descuentan
 * inventario. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoMovimientosIntegracionTest {

    private static final String RUTA = "/api/finanzas/movimientos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private EntityManager entityManager;

    private String token;
    private LocalDate hoy;
    private Map<String, Integer> tipos;

    @BeforeEach
    void preparar() throws Exception {
        token = json.readTree(mvc.perform(post("/api/auth/inicio-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombreUsuario", "admin", "contrasena", "admin123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("tokenAcceso").asString();
        hoy = LocalDate.now();
        tipos = new HashMap<>();
        for (JsonNode tipo : leer(get("/api/finanzas/tipos-movimiento"))) {
            tipos.put(tipo.get("nombre").asString(), tipo.get("id").asInt());
        }
    }

    @Test
    void imputaAProyectoYEventoYExigeProveedorEnLosEgresos() throws Exception {
        int[] proyectoYEvento = proyectoConEvento();
        int idProyecto = proyectoYEvento[0];
        Map<String, Object> cobro = movimiento("Cobro de servicios", "Anticipo del 50 %", "2110.00");
        cobro.put("idProyecto", idProyecto);
        cobro.put("idEvento", proyectoYEvento[1]);
        JsonNode registrado = cuerpo(ejecutar(post(RUTA), cobro).andExpect(status().isCreated()));
        assertThat(registrado.get("evento").get("nombre").asString()).isEqualTo("Inauguración");

        Map<String, Object> sinProyecto = movimiento("Cobro de servicios", "Anticipo", "100.00");
        sinProyecto.put("idEvento", proyectoYEvento[1]);
        ejecutar(post(RUTA), sinProyecto).andExpect(status().isUnprocessableContent());

        Map<String, Object> subcontrato = movimiento("Subcontratación de servicios", "Generador eléctrico", "800");
        subcontrato.put("idProyecto", idProyecto);
        ejecutar(post(RUTA), subcontrato).andExpect(status().isUnprocessableContent());
        subcontrato.put("idProveedor", crear("/api/finanzas/proveedores", Map.of("razonSocial",
                "Generadores de prueba S.R.L.", "servicios", List.of("Generador eléctrico"))));
        ejecutar(post(RUTA), subcontrato).andExpect(status().isCreated());
        ejecutar(post(RUTA), movimiento("Gastos generales", "Energía eléctrica de la oficina", "350.40"))
                .andExpect(status().isCreated());

        JsonNode delProyecto = leer(get(RUTA).param("idProyecto", String.valueOf(idProyecto)));
        assertThat(delProyecto.get("pagina").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(delProyecto.get("ingresos").decimalValue()).isEqualByComparingTo("2110.00");
        assertThat(delProyecto.get("egresos").decimalValue()).isEqualByComparingTo("800.00");
        assertThat(delProyecto.get("saldo").decimalValue()).isEqualByComparingTo("1310.00");

        int idCobro = registrado.get("id").asInt();
        ejecutar(post(RUTA + "/" + idCobro + "/anulacion"), Map.of("motivo", "Registrado dos veces"))
                .andExpect(status().isOk());
        nuevaSolicitud();
        assertThat(leer(get(RUTA).param("idProyecto", String.valueOf(idProyecto))).get("ingresos").decimalValue())
                .isEqualByComparingTo("0");
    }

    @Test
    void laVentaDescuentaElInventarioYSuAnulacionLoRepone() throws Exception {
        int idBodega = leer(get("/api/activos/catalogos/bodegas")).get(0).get("id").asInt();
        int idCategoria = leer(get("/api/activos/catalogos/categorias")).get(0).get("id").asInt();
        int idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante finanzas"));
        int idModeloUnidad = crear("/api/activos/modelos", Map.of("idFabricante", idFabricante, "idCategoria",
                idCategoria, "nombre", "Consola de venta", "tipoControl", "UNIDAD", "prefijoCodigo", "FNZ"));
        int idModeloLote = crear("/api/activos/modelos", Map.of("idFabricante", idFabricante, "idCategoria",
                idCategoria, "nombre", "Cable de venta", "tipoControl", "LOTE"));
        int idActivo = cuerpo(ejecutar(post("/api/activos"), Map.of("idModelo", idModeloUnidad, "idBodega", idBodega,
                "cantidad", 1)).andExpect(status().isCreated())).get(0).get("id").asInt();
        int idExistencia = cuerpo(ejecutar(post("/api/activos/lotes/movimientos"), Map.of("idModelo", idModeloLote,
                "idBodega", idBodega, "tipo", "INGRESO", "cantidad", 10)).andExpect(status().isCreated()))
                .get("id").asInt();
        ejecutar(post("/api/activos/lotes/movimientos"), Map.of("idModelo", idModeloLote, "idBodega", idBodega,
                "tipo", "VENTA", "cantidad", 1)).andExpect(status().isUnprocessableContent());

        // La prueba comparte una transaccion: la venta fallida solo incluye el lote para no dejar
        // cambios parciales visibles (en produccion la peticion completa se revierte)
        ejecutar(post(RUTA), venta(null, idExistencia, 20)).andExpect(status().isUnprocessableContent());
        nuevaSolicitud();
        assertThat(cantidadLote(idModeloLote)).isEqualTo(10);

        JsonNode venta = cuerpo(ejecutar(post(RUTA), venta(idActivo, idExistencia, 4)).andExpect(status().isCreated()));
        assertThat(venta.get("items")).hasSize(2);
        nuevaSolicitud();
        assertThat(estadoActivo(idActivo)).isEqualTo("VENDIDO");
        assertThat(cantidadLote(idModeloLote)).isEqualTo(6);
        ejecutar(post(RUTA), venta(idActivo, null, 0)).andExpect(status().isUnprocessableContent());

        ejecutar(post(RUTA + "/" + venta.get("id").asInt() + "/anulacion"), Map.of("motivo", "El cliente desistió"))
                .andExpect(status().isOk());
        nuevaSolicitud();
        assertThat(estadoActivo(idActivo)).isEqualTo("DISPONIBLE");
        assertThat(cantidadLote(idModeloLote)).isEqualTo(10);
    }

    private Map<String, Object> venta(Integer idActivo, Integer idExistencia, int cantidad) {
        Map<String, Object> venta = movimiento("Venta de equipos", "Venta de consola usada", "15000");
        List<Map<String, Object>> items = new ArrayList<>();
        if (idActivo != null) {
            items.add(Map.of("idActivo", idActivo));
        }
        if (idExistencia != null) {
            items.add(Map.of("idExistencia", idExistencia, "cantidad", cantidad));
        }
        venta.put("items", items);
        return venta;
    }

    private String estadoActivo(int idActivo) throws Exception {
        return leer(get("/api/activos/{id}", idActivo)).get("estado").asString();
    }

    private int cantidadLote(int idModelo) throws Exception {
        for (JsonNode existencia : leer(get("/api/activos/lotes").param("texto", "Cable de venta"))) {
            if (existencia.get("idModelo").asInt() == idModelo) {
                return existencia.get("cantidad").asInt();
            }
        }
        throw new AssertionError("Sin existencia del modelo " + idModelo);
    }

    private int[] proyectoConEvento() throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Servicio finanzas de prueba",
                "unidadMedida", "jornada", "precioReferencial", 2110, "activo", true));
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        int idCliente = crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente finanzas de prueba", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "770011003"))));
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Inauguración de prueba");
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", hoy.plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", hoy.plusDays(10).toString());
        cotizacion.put("fechaEventoFin", hoy.plusDays(10).toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 4220,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        int idProyecto = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion))).andExpect(status().isOk())).get("idProyecto").asInt();
        int idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Salón finanzas de prueba",
                "direccion", "Av. Arce 2177", "ciudad", "La Paz"));
        int idEvento = crear("/api/comercial/proyectos/" + idProyecto + "/eventos", Map.of("idLugar", idLugar,
                "nombre", "Inauguración", "fechaInicio", hoy.plusDays(10) + "T19:00:00",
                "fechaFin", hoy.plusDays(10) + "T23:00:00"));
        return new int[] {idProyecto, idEvento};
    }

    private Map<String, Object> movimiento(String tipo, String concepto, String monto) {
        Map<String, Object> movimiento = new HashMap<>();
        movimiento.put("idTipo", tipos.get(tipo));
        movimiento.put("concepto", concepto);
        movimiento.put("monto", monto);
        movimiento.put("fecha", hoy.toString());
        return movimiento;
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
        return cuerpo(ejecutar(post(ruta), datos).andExpect(status().isCreated())).get("id").asInt();
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> datos) throws Exception {
        return mvc.perform(autorizado(solicitud).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos)));
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
