package com.visual.backend.logistica.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujo de logistica contra PostgreSQL (base de pruebas): jornadas de montaje,
 * asignacion de personal con alertas de cruce, permisos del gerente y
 * consulta de asignaciones propias. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoLogisticaIntegracionTest {

    private static final int ID_ROL_GERENTE = 2;
    private static final int ID_ROL_TECNICO = 3;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String RUTA_EVENTO = "/api/logistica/eventos/{id}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    private String token;
    private LocalDate dia;
    private int idEventoGala;
    private int idEventoPrensa;
    private int idTecnicoSonido;
    private JsonNode catalogos;

    @BeforeEach
    void prepararDatos() throws Exception {
        token = iniciarSesion("admin", "admin123456");
        dia = LocalDate.now().plusDays(10);
        int idProyecto = proyectoAceptado();
        idEventoGala = crear("/api/comercial/proyectos/" + idProyecto + "/eventos",
                evento("Cena de gala", hora(19), hora(23)));
        idEventoPrensa = crear("/api/comercial/proyectos/" + idProyecto + "/eventos",
                evento("Conferencia de prensa", hora(20), hora(22)));
        catalogos = leer(get("/api/logistica/catalogos"), token);
        idTecnicoSonido = crear("/api/logistica/personal", personal("Juan Carlos", "Mamani Quispe", "4876512 LP",
                null));
    }

    @Test
    void planificaElMontajeYAlertaLosCrucesSinBloquearlos() throws Exception {
        String rutaJornadas = "/api/logistica/eventos/" + idEventoGala + "/jornadas";
        ejecutar(post(rutaJornadas), jornada("MONTAJE", hora(8), hora(17))).andExpect(status().isCreated());
        ejecutar(post(rutaJornadas), jornada("MONTAJE", hora(17), hora(20)))
                .andExpect(status().isUnprocessableContent());

        JsonNode enGala = cuerpo(ejecutar(post("/api/logistica/eventos/" + idEventoGala + "/asignaciones"),
                asignacion(idTecnicoSonido, hora(8), hora(23))).andExpect(status().isCreated()));
        assertThat(enGala.get("cruces")).isEmpty();
        ejecutar(post("/api/logistica/eventos/" + idEventoGala + "/asignaciones"),
                asignacion(idTecnicoSonido, hora(9), hora(10))).andExpect(status().isUnprocessableContent());
        ejecutar(post("/api/logistica/eventos/" + idEventoGala + "/asignaciones"),
                asignacion(idTecnicoSonido, hora(7), hora(10))).andExpect(status().isUnprocessableContent());

        JsonNode enPrensa = cuerpo(ejecutar(post("/api/logistica/eventos/" + idEventoPrensa + "/asignaciones"),
                asignacion(idTecnicoSonido, hora(20), hora(22))).andExpect(status().isCreated()));
        assertThat(enPrensa.get("cruces")).singleElement()
                .satisfies(cruce -> assertThat(cruce.get("evento").asString()).isEqualTo("Cena de gala"));

        JsonNode plan = leer(get(RUTA_EVENTO, idEventoGala), token);
        assertThat(plan.get("ventanaInicio").asString()).isEqualTo(hora(8));
        assertThat(plan.get("asignaciones").get(0).get("cruces")).hasSize(1);
        int idJornada = plan.get("jornadas").get(0).get("id").asInt();
        mvc.perform(autorizado(delete(rutaJornadas + "/" + idJornada), token))
                .andExpect(status().isUnprocessableContent());

        JsonNode listado = leer(get("/api/logistica/eventos").param("desde", dia.toString())
                .param("hasta", dia.toString()).param("texto", "cena"), token);
        assertThat(listado.get("contenido")).singleElement().satisfies(fila -> {
            assertThat(fila.get("jornadas").asInt()).isEqualTo(1);
            assertThat(fila.get("personal").asInt()).isEqualTo(1);
            assertThat(fila.get("cruces").asInt()).isEqualTo(1);
        });

        JsonNode disponibles = leer(get("/api/logistica/eventos/" + idEventoPrensa + "/personal-disponible")
                .param("inicio", hora(20)).param("fin", hora(21)), token);
        assertThat(disponibles).anySatisfy(persona -> {
            assertThat(persona.get("id").asInt()).isEqualTo(idTecnicoSonido);
            assertThat(persona.get("cruces")).hasSize(1);
        });
    }

    @Test
    void elTecnicoVinculadoConsultaSoloSusAsignaciones() throws Exception {
        int idUsuario = crear("/api/usuarios", usuario("tecnico.logistica", ID_ROL_TECNICO));
        String tokenTecnico = cambiarContrasena(iniciarSesion("tecnico.logistica", CONTRASENA_TEMPORAL));
        assertThat(leer(get("/api/logistica/mis-asignaciones"), tokenTecnico).get("vinculado").asBoolean()).isFalse();

        int idPersonal = crear("/api/logistica/personal", personal("Rubén", "Condori Apaza", "6123456 LP",
                idUsuario));
        ejecutar(post("/api/logistica/personal"), personal("Otro", "Técnico", "7000001 LP", idUsuario))
                .andExpect(status().isConflict());
        ejecutar(post("/api/logistica/eventos/" + idEventoGala + "/asignaciones"),
                asignacion(idPersonal, hora(19), hora(23))).andExpect(status().isCreated());
        ejecutar(put("/api/logistica/eventos/" + idEventoGala + "/transporte"),
                Map.of("detallesTransporte", "Camión 1 sale de Bodega 1 a las 07:30")).andExpect(status().isOk());

        JsonNode mias = leer(get("/api/logistica/mis-asignaciones"), tokenTecnico);
        assertThat(mias.get("vinculado").asBoolean()).isTrue();
        assertThat(mias.get("asignaciones")).singleElement().satisfies(propia -> {
            assertThat(propia.get("evento").get("nombre").asString()).isEqualTo("Cena de gala");
            assertThat(propia.get("detallesTransporte").asString()).contains("Bodega 1");
        });
        mvc.perform(autorizado(get("/api/logistica/eventos"), tokenTecnico)).andExpect(status().isForbidden());

        Map<String, Object> inactivo = personal("Rubén", "Condori Apaza", "6123456 LP", idUsuario);
        inactivo.put("activo", false);
        ejecutar(put("/api/logistica/personal/" + idPersonal), inactivo).andExpect(status().isUnprocessableContent());
    }

    @Test
    void elGerentePlanificaLaLogistica() throws Exception {
        crear("/api/usuarios", usuario("gerente.logistica", ID_ROL_GERENTE));
        String tokenGerente = cambiarContrasena(iniciarSesion("gerente.logistica", CONTRASENA_TEMPORAL));

        mvc.perform(autorizado(post("/api/logistica/eventos/" + idEventoGala + "/jornadas"), tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(jornada("MONTAJE", hora(9), hora(18)))))
                .andExpect(status().isCreated());
        mvc.perform(autorizado(post("/api/logistica/personal"), tokenGerente).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(personal("Ana", "Torrez", "4876512 LP", null))))
                .andExpect(status().isConflict());
    }

    private int proyectoAceptado() throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Sonido de prueba logística",
                "unidadMedida", "jornada", "precioReferencial", 4500, "activo", true));
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente"), token), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento"), token), "NIT");
        int idCliente = crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente de prueba logística", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "880011002"))));
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Gala de prueba");
        cotizacion.put("fechaEmision", LocalDate.now().toString());
        cotizacion.put("fechaValidez", LocalDate.now().plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", dia.toString());
        cotizacion.put("fechaEventoFin", dia.toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 4500,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        return cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion", idCotizacion), token))
                .andExpect(status().isOk())).get("idProyecto").asInt();
    }

    private Map<String, Object> evento(String nombre, String inicio, String fin) throws Exception {
        int idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Salón " + nombre,
                "direccion", "Av. 16 de Julio 1800", "ciudad", "La Paz"));
        return Map.of("idLugar", idLugar, "nombre", nombre, "fechaInicio", inicio, "fechaFin", fin);
    }

    private Map<String, Object> personal(String nombres, String apellidos, String ci, Integer idUsuario) {
        Map<String, Object> personal = new HashMap<>();
        personal.put("nombres", nombres);
        personal.put("apellidos", apellidos);
        personal.put("telefono", "71234567");
        personal.put("idTipoDocumento", idPorNombre(catalogos.get("tiposDocumento"), "Carnet de identidad"));
        personal.put("numeroDocumento", ci);
        personal.put("idsEspecialidades", List.of(idPorNombre(catalogos.get("especialidades"), "Sonido")));
        personal.put("idUsuario", idUsuario);
        personal.put("activo", true);
        return personal;
    }

    private Map<String, Object> asignacion(int idPersonal, String inicio, String fin) {
        return Map.of("idPersonal", idPersonal, "idRol", idPorNombre(catalogos.get("rolesEvento"),
                "Técnico de sonido"), "inicio", inicio, "fin", fin);
    }

    private static Map<String, Object> jornada(String tipo, String inicio, String fin) {
        return Map.of("tipo", tipo, "inicio", inicio, "fin", fin, "descripcion", "Armado de escenario");
    }

    private static Map<String, Object> usuario(String nombreUsuario, int idRol) {
        return Map.of("nombreUsuario", nombreUsuario, "nombres", "Usuario", "apellidoPaterno", "Logística",
                "email", nombreUsuario + "@visual.com.bo", "idRol", idRol, "contrasenaTemporal", CONTRASENA_TEMPORAL);
    }

    private String hora(int hora) {
        return dia + String.format("T%02d:00:00", hora);
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
