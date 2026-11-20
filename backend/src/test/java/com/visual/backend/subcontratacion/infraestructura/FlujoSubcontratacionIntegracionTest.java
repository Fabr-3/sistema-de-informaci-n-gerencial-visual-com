package com.visual.backend.subcontratacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Subcontrataciones con su egreso, stock de consumibles, consumos de
 * tecnicos y costo en el margen del proyecto, contra PostgreSQL. Cada prueba
 * se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoSubcontratacionIntegracionTest {

    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String RUTA_SUBCONTRATACIONES = "/api/subcontratacion/subcontrataciones";
    private static final String RUTA_CONSUMIBLES = "/api/subcontratacion/consumibles";
    private static final String RUTA_CONSUMOS = "/api/subcontratacion/consumos";
    private static final String RUTA_GASTOS = "/api/subcontratacion/gastos-evento";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private EntityManager entityManager;

    private String token;
    private LocalDate hoy;
    private int idProyecto;
    private int idEvento;
    private int idProveedor;

    @BeforeEach
    void prepararDatos() throws Exception {
        token = iniciarSesion("admin", "admin123456");
        hoy = LocalDate.now();
        int[] proyectoYEvento = proyectoConEvento();
        idProyecto = proyectoYEvento[0];
        idEvento = proyectoYEvento[1];
        idProveedor = crear("/api/finanzas/proveedores", Map.of("razonSocial", "Generadores Illimani S.R.L.",
                "servicios", List.of("Generador de 60 kVA", "Operador de generador")));
    }

    @Test
    void laSubcontratacionCreaSuEgresoYAnularlaLoAnula() throws Exception {
        JsonNode servicios = leer(get("/api/subcontratacion/proveedores/{id}/servicios", idProveedor));
        int idServicio = servicios.get(0).get("id").asInt();

        JsonNode registrada = cuerpo(ejecutar(post(RUTA_SUBCONTRATACIONES), subcontratacion(idServicio, "1800"))
                .andExpect(status().isCreated()));

        assertThat(registrada.get("servicio").get("nombre").asString()).isEqualTo("Generador de 60 kVA");
        int idMovimiento = registrada.get("idMovimiento").asInt();
        JsonNode egreso = leer(get("/api/finanzas/movimientos/{id}", idMovimiento));
        assertThat(egreso.get("tipo").get("codigoSistema").asString()).isEqualTo("SUBCONTRATACION");
        assertThat(egreso.get("monto").decimalValue()).isEqualByComparingTo("1800");
        assertThat(egreso.get("proyecto").get("id").asInt()).isEqualTo(idProyecto);
        assertThat(egreso.get("evento").get("id").asInt()).isEqualTo(idEvento);
        assertThat(egreso.get("proveedor").get("id").asInt()).isEqualTo(idProveedor);

        ejecutar(post("/api/finanzas/movimientos/{id}/anulacion", idMovimiento), Map.of("motivo", "Error"))
                .andExpect(status().isUnprocessableContent());

        ejecutar(post(RUTA_SUBCONTRATACIONES + "/{id}/anulacion", registrada.get("id").asInt()),
                Map.of("motivo", "El proveedor no se presentó")).andExpect(status().isOk());
        nuevaSolicitud();
        JsonNode anulado = leer(get("/api/finanzas/movimientos/{id}", idMovimiento));
        assertThat(anulado.get("estado").asString()).isEqualTo("ANULADO");
        assertThat(anulado.get("motivoAnulacion").asString()).isEqualTo("El proveedor no se presentó");
    }

    @Test
    void elSubalquilerDeEquiposFiguraEnElEventoYSuEgresoEnFinanzas() throws Exception {
        int idCategoria = leer(get("/api/activos/catalogos/categorias")).get(0).get("id").asInt();
        int idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante subalquiler"));
        Map<String, Object> modelo = new HashMap<>(Map.of("idFabricante", idFabricante, "idCategoria", idCategoria,
                "nombre", "Cabezal móvil", "tipoControl", "UNIDAD"));
        modelo.put("prefijoCodigo", "SBQ");
        int idModelo = crear("/api/activos/modelos", modelo);
        Map<String, Object> subalquiler = new HashMap<>(Map.of("idProveedor", idProveedor, "idProyecto", idProyecto,
                "descripcion", "Faltaban unidades para el evento", "costo", "900", "fecha", hoy.toString(),
                "idModelo", idModelo, "cantidad", 3));

        ejecutar(post(RUTA_SUBCONTRATACIONES), subalquiler).andExpect(status().isUnprocessableContent());
        subalquiler.put("idEvento", idEvento);
        JsonNode registrado = cuerpo(ejecutar(post(RUTA_SUBCONTRATACIONES), subalquiler)
                .andExpect(status().isCreated()));

        assertThat(registrado.get("cantidad").asInt()).isEqualTo(3);
        JsonNode egreso = leer(get("/api/finanzas/movimientos/{id}", registrado.get("idMovimiento").asInt()));
        assertThat(egreso.get("concepto").asString()).contains("Subalquiler").contains("3 × Fabricante subalquiler");
        assertThat(egreso.get("evento").get("id").asInt()).isEqualTo(idEvento);
        assertThat(leer(get("/api/logistica/eventos/{id}/subalquileres", idEvento))).singleElement()
                .satisfies(equipo -> {
                    assertThat(equipo.get("cantidad").asInt()).isEqualTo(3);
                    assertThat(equipo.get("modelo").asString()).isEqualTo("Fabricante subalquiler Cabezal móvil");
                });
    }

    @Test
    void elTipoDeSubcontratacionNoSeRegistraAManoEnFinanzas() throws Exception {
        int idTipo = 0;
        for (JsonNode tipo : leer(get("/api/finanzas/tipos-movimiento"))) {
            if ("SUBCONTRATACION".equals(tipo.path("codigoSistema").asString(""))) {
                idTipo = tipo.get("id").asInt();
            }
        }
        Map<String, Object> manual = new HashMap<>(Map.of("idTipo", idTipo, "concepto", "Generador", "monto", "500",
                "fecha", hoy.toString(), "idProyecto", idProyecto, "idProveedor", idProveedor));

        ejecutar(post("/api/finanzas/movimientos"), manual).andExpect(status().isUnprocessableContent());
    }

    @Test
    void unConsumoSinStockSuficienteNoRegistraNada() throws Exception {
        int idConsumible = consumibleConStock("Cinta gaffer negra", "4", "45");

        JsonNode error = cuerpo(ejecutar(post(RUTA_CONSUMOS), consumo(idConsumible, "5"))
                .andExpect(status().isUnprocessableContent()));

        assertThat(error.get("detail").asString()).contains("disponible 4.00 rollo");
        assertThat(leer(get(RUTA_CONSUMIBLES + "/{id}", idConsumible)).get("stockActual").decimalValue())
                .isEqualByComparingTo("4");
        assertThat(leer(get(RUTA_CONSUMOS).param("idConsumible", String.valueOf(idConsumible)))
                .get("totalElementos").asInt()).isZero();
    }

    @Test
    void elTecnicoRegistraGastosDelEventoConComprobanteYSuEgreso() throws Exception {
        int idUsuario = crear("/api/usuarios", Map.of("nombreUsuario", "tecnico.gastos", "nombres", "Técnico",
                "apellidoPaterno", "Gastos", "email", "tecnico.gastos@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasena(iniciarSesion("tecnico.gastos", CONTRASENA_TEMPORAL));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};

        registrarGasto(tokenTecnico, "recibo.png", "image/png", png).andExpect(status().isForbidden());
        asignar(idUsuario);
        registrarGasto(tokenTecnico, "recibo.png", "image/png", "no es imagen".getBytes())
                .andExpect(status().isUnprocessableContent());
        JsonNode gasto = cuerpo(registrarGasto(tokenTecnico, "recibo.png", "image/png", png)
                .andExpect(status().isCreated()));

        assertThat(gasto.get("comprobante").asString()).isEqualTo("recibo.png");
        int idMovimiento = gasto.get("idMovimiento").asInt();
        JsonNode egreso = leer(get("/api/finanzas/movimientos/{id}", idMovimiento));
        assertThat(egreso.get("tipo").get("codigoSistema").asString()).isEqualTo("GASTO_EVENTO");
        assertThat(egreso.get("monto").decimalValue()).isEqualByComparingTo("85.50");
        assertThat(egreso.get("evento").get("id").asInt()).isEqualTo(idEvento);
        ejecutar(post("/api/finanzas/movimientos/{id}/anulacion", idMovimiento), Map.of("motivo", "Error"))
                .andExpect(status().isUnprocessableContent());
        byte[] descargado = mvc.perform(autorizado(get(RUTA_GASTOS + "/{id}/comprobante", gasto.get("id").asInt()),
                        tokenTecnico)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(descargado).isEqualTo(png);
        assertThat(leer(get(RUTA_GASTOS), tokenTecnico).get("totalElementos").asInt()).isEqualTo(1);

        ejecutar(post(RUTA_GASTOS + "/{id}/anulacion", gasto.get("id").asInt()),
                Map.of("motivo", "Se registró dos veces"), tokenTecnico).andExpect(status().isOk());
        nuevaSolicitud();
        assertThat(leer(get("/api/finanzas/movimientos/{id}", idMovimiento)).get("estado").asString())
                .isEqualTo("ANULADO");
    }

    private ResultActions registrarGasto(String tokenUsuario, String nombre, String tipo, byte[] contenido)
            throws Exception {
        MockMultipartFile datos = new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                json.writeValueAsBytes(Map.of("idProyecto", idProyecto, "idEvento", idEvento,
                        "descripcion", "Repuesto de cable de energía", "monto", "85.50", "fecha", hoy.toString())));
        MockMultipartFile archivo = new MockMultipartFile("archivo", nombre, tipo, contenido);
        return mvc.perform(multipart(RUTA_GASTOS).file(datos).file(archivo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUsuario));
    }

    @Test
    void elTecnicoSoloRegistraConsumosEnLosEventosDondeEstaAsignado() throws Exception {
        int idConsumible = consumibleConStock("Pilas AA alcalinas", "40", "6.50");
        int idUsuario = crear("/api/usuarios", Map.of("nombreUsuario", "tecnico.consumos", "nombres", "Técnico",
                "apellidoPaterno", "Consumos", "email", "tecnico.consumos@visual.com.bo", "idRol", ID_ROL_TECNICO,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenTecnico = cambiarContrasena(iniciarSesion("tecnico.consumos", CONTRASENA_TEMPORAL));

        ejecutar(post(RUTA_CONSUMOS), consumo(idConsumible, "4"), tokenTecnico).andExpect(status().isForbidden());

        asignar(idUsuario);
        assertThat(leer(get(RUTA_CONSUMOS + "/eventos-asignados"), tokenTecnico)).singleElement()
                .satisfies(evento -> assertThat(evento.get("idEvento").asInt()).isEqualTo(idEvento));
        ejecutar(post(RUTA_CONSUMOS), consumo(idConsumible, "4"), tokenTecnico).andExpect(status().isCreated());
        assertThat(leer(get(RUTA_CONSUMOS), tokenTecnico).get("contenido")).singleElement()
                .satisfies(fila -> assertThat(fila.get("registradoPor").asString()).isEqualTo("Técnico Consumos"));
        ejecutar(post(RUTA_CONSUMIBLES), consumible("Cinta aislante"), tokenTecnico)
                .andExpect(status().isForbidden());
    }

    @Test
    void elMargenDelProyectoIncluyeLosConsumibles() throws Exception {
        int idConsumible = consumibleConStock("Cinta gaffer blanca", "10", "45");
        int idTipoCobro = 0;
        for (JsonNode tipo : leer(get("/api/finanzas/tipos-movimiento"))) {
            if ("Cobro de servicios".equals(tipo.get("nombre").asString())) {
                idTipoCobro = tipo.get("id").asInt();
            }
        }
        crear("/api/finanzas/movimientos", Map.of("idTipo", idTipoCobro, "concepto", "Anticipo", "monto", "1000",
                "fecha", hoy.toString(), "idProyecto", idProyecto));
        crear(RUTA_CONSUMOS, consumo(idConsumible, "2"));
        int idAnulado = crear(RUTA_CONSUMOS, consumo(idConsumible, "3"));
        ejecutar(post(RUTA_CONSUMOS + "/{id}/anulacion", idAnulado), Map.of("motivo", "Se registró dos veces"))
                .andExpect(status().isOk());
        assertThat(leer(get(RUTA_CONSUMIBLES + "/{id}", idConsumible)).get("stockActual").decimalValue())
                .isEqualByComparingTo("8");
        nuevaSolicitud();

        JsonNode detalle = leer(get("/api/finanzas/resultados/proyectos/{id}", idProyecto));

        assertThat(detalle.get("proyecto").get("margen").get("costos").decimalValue()).isEqualByComparingTo("90");
        assertThat(detalle.get("proyecto").get("margen").get("margen").decimalValue()).isEqualByComparingTo("910");
        assertThat(detalle.get("porTipo")).anySatisfy(linea -> {
            assertThat(linea.get("tipo").asString()).isEqualTo("Consumibles");
            assertThat(linea.get("monto").decimalValue()).isEqualByComparingTo("90");
        });
        assertThat(detalle.get("eventos")).anySatisfy(evento -> {
            assertThat(evento.get("idEvento").asInt()).isEqualTo(idEvento);
            assertThat(evento.get("margen").get("costos").decimalValue()).isEqualByComparingTo("90");
        });
    }

    private int consumibleConStock(String nombre, String cantidad, String costo) throws Exception {
        Map<String, Object> datos = consumible(nombre);
        datos.put("costoUnitario", costo);
        int id = crear(RUTA_CONSUMIBLES, datos);
        ejecutar(post(RUTA_CONSUMIBLES + "/{id}/ingresos", id), Map.of("cantidad", cantidad, "motivo", "Compra"))
                .andExpect(status().isOk());
        return id;
    }

    private static Map<String, Object> consumible(String nombre) {
        return new HashMap<>(Map.of("nombre", nombre, "unidadMedida", "rollo", "stockMinimo", "2",
                "costoUnitario", "10", "activo", true));
    }

    private Map<String, Object> consumo(int idConsumible, String cantidad) {
        return Map.of("idConsumible", idConsumible, "idProyecto", idProyecto, "idEvento", idEvento,
                "cantidad", cantidad, "fecha", hoy.toString());
    }

    private Map<String, Object> subcontratacion(int idServicio, String costo) {
        return Map.of("idProveedor", idProveedor, "idServicio", idServicio, "idProyecto", idProyecto,
                "idEvento", idEvento, "descripcion", "Generador para el escenario principal", "costo", costo,
                "fecha", hoy.toString());
    }

    private void asignar(int idUsuario) throws Exception {
        JsonNode catalogos = leer(get("/api/logistica/catalogos"));
        Map<String, Object> personal = new HashMap<>();
        personal.put("nombres", "Técnico");
        personal.put("apellidos", "Consumos");
        personal.put("telefono", "71234567");
        personal.put("idTipoDocumento", catalogos.get("tiposDocumento").get(0).get("id").asInt());
        personal.put("numeroDocumento", "9001234 LP");
        personal.put("idsEspecialidades", List.of(catalogos.get("especialidades").get(0).get("id").asInt()));
        personal.put("idUsuario", idUsuario);
        personal.put("activo", true);
        int idPersonal = crear("/api/logistica/personal", personal);
        String dia = hoy.plusDays(10).toString();
        crear("/api/logistica/eventos/" + idEvento + "/asignaciones", Map.of("idPersonal", idPersonal,
                "idRol", catalogos.get("rolesEvento").get(0).get("id").asInt(), "inicio", dia + "T19:00:00",
                "fin", dia + "T23:00:00"));
    }

    private int[] proyectoConEvento() throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Sonido de prueba consumos",
                "unidadMedida", "jornada", "precioReferencial", 5000, "activo", true));
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        int idCliente = crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente de prueba consumos", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "770011099"))));
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Concierto de prueba");
        cotizacion.put("fechaEmision", hoy.toString());
        cotizacion.put("fechaValidez", hoy.plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", hoy.plusDays(10).toString());
        cotizacion.put("fechaEventoFin", hoy.plusDays(10).toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 5000,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        int proyecto = cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion",
                idCotizacion), token)).andExpect(status().isOk())).get("idProyecto").asInt();
        int idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Salón consumos de prueba",
                "direccion", "Av. 6 de Agosto 2500", "ciudad", "La Paz"));
        int evento = crear("/api/comercial/proyectos/" + proyecto + "/eventos", Map.of("idLugar", idLugar,
                "nombre", "Función principal", "fechaInicio", hoy.plusDays(10) + "T19:00:00",
                "fechaFin", hoy.plusDays(10) + "T23:00:00"));
        return new int[] {proyecto, evento};
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
        return ejecutar(solicitud, datos, token);
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, Map<String, ?> datos, String tokenUsuario)
            throws Exception {
        return mvc.perform(autorizado(solicitud, tokenUsuario).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(datos)));
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud) throws Exception {
        return leer(solicitud, token);
    }

    private JsonNode leer(MockHttpServletRequestBuilder solicitud, String tokenUsuario) throws Exception {
        return cuerpo(mvc.perform(autorizado(solicitud, tokenUsuario)).andExpect(status().isOk()));
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
