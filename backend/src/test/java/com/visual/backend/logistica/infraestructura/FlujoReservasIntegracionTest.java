package com.visual.backend.logistica.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reserva de equipos por evento contra PostgreSQL: todo o nada ante
 * conflictos, recalculo al cambiar jornadas o el evento, liberacion al
 * cancelar, bloqueo de la baja y permisos. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlujoReservasIntegracionTest {

    private static final int ID_ROL_GERENTE = 2;
    private static final String CONTRASENA_TEMPORAL = "temporal123";
    private static final String RUTA_RESERVAS = "/api/logistica/eventos/{id}/reservas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private LocalDate dia;
    private int idProyecto;
    private int idLugar;
    private int idGala;
    private int idAlmuerzo;
    private int idPantalla;
    private int idProcesador;
    private String codigoPantalla;

    @BeforeEach
    void prepararDatos() throws Exception {
        token = iniciarSesion("admin", "admin123456");
        dia = LocalDate.now().plusDays(12);
        idProyecto = proyectoAceptado();
        idLugar = crear("/api/comercial/catalogos/lugares", Map.of("nombre", "Salón reservas de prueba",
                "direccion", "Av. Arce 2500", "ciudad", "La Paz"));
        idGala = crearEvento("Cena de gala", 19, 23);
        idAlmuerzo = crearEvento("Almuerzo de prensa", 12, 15);
        JsonNode equipos = registrarEquipos(2);
        idPantalla = equipos.get(0).get("id").asInt();
        codigoPantalla = equipos.get(0).get("codigoInventario").asString();
        idProcesador = equipos.get(1).get("id").asInt();
    }

    @Test
    void reservaSinConflictoYElEquipoSigueLibreParaOtroHorario() throws Exception {
        JsonNode reservadas = cuerpo(reservar(idGala, idPantalla).andExpect(status().isCreated()));

        assertThat(reservadas).singleElement().satisfies(reserva -> {
            assertThat(reserva.get("codigo").asString()).isEqualTo(codigoPantalla);
            assertThat(reserva.get("salida").asString()).isEqualTo(hora(19));
            assertThat(reserva.get("retorno").asString()).isEqualTo(hora(23));
        });
        assertThat(leer(get(RUTA_RESERVAS, idGala))).hasSize(1);
        assertThat(ids(leer(get("/api/logistica/eventos/{id}/activos-disponibles", idGala))))
                .doesNotContain(idPantalla).contains(idProcesador);
        assertThat(ids(leer(get("/api/logistica/eventos/{id}/activos-disponibles", idAlmuerzo))))
                .contains(idPantalla);
        assertThat(leer(get("/api/activos/{id}/reservas", idPantalla))).singleElement()
                .satisfies(reserva -> assertThat(reserva.get("evento").asString()).isEqualTo("Cena de gala"));
        assertThat(jdbc.queryForObject("select estado_operativo from activos where id_activo = ?", String.class,
                idPantalla)).isEqualTo("DISPONIBLE");

        reservar(idGala, idPantalla).andExpect(status().isUnprocessableContent());
    }

    @Test
    void unConflictoRechazaTodaLaReservaYDetallaQuienOcupaElEquipo() throws Exception {
        reservar(idGala, idPantalla).andExpect(status().isCreated());
        int idBrindis = crearEvento("Brindis de cierre", 20, 22);

        JsonNode error = cuerpo(reservar(idBrindis, idProcesador, idPantalla).andExpect(status().isConflict()));

        assertThat(error.get("conflictos")).singleElement().satisfies(conflicto -> {
            assertThat(conflicto.get("codigoActivo").asString()).isEqualTo(codigoPantalla);
            assertThat(conflicto.get("evento").asString()).isEqualTo("Cena de gala");
            assertThat(conflicto.get("salida").asString()).isEqualTo(hora(19));
        });
        assertThat(leer(get(RUTA_RESERVAS, idBrindis))).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from reservas_activo where id_activo = ?", Integer.class,
                idProcesador)).isZero();
    }

    @Test
    void laBaseDeDatosRechazaReservasSuperpuestasAunqueSeOmitaElServicio() throws Exception {
        reservar(idGala, idPantalla).andExpect(status().isCreated());
        ejecutar(put("/api/logistica/eventos/{id}/transporte", idAlmuerzo),
                Map.of("detallesTransporte", "Camioneta 2")).andExpect(status().isOk());
        Integer idPlanAlmuerzo = jdbc.queryForObject("select id_plan from planes_logisticos where id_evento = ?",
                Integer.class, idAlmuerzo);

        assertThatThrownBy(() -> jdbc.update("""
                insert into reservas_activo (id_plan, id_activo, fecha_salida, fecha_retorno)
                values (?, ?, ?::timestamp, ?::timestamp)
                """, idPlanAlmuerzo, idPantalla, hora(12), hora(20)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unaJornadaQueProvocaConflictoNoSeGuarda() throws Exception {
        reservar(idGala, idPantalla).andExpect(status().isCreated());
        reservar(idAlmuerzo, idPantalla).andExpect(status().isCreated());
        String rutaJornadas = "/api/logistica/eventos/" + idGala + "/jornadas";

        ejecutar(post(rutaJornadas), jornada("MONTAJE", 16, 18)).andExpect(status().isCreated());
        assertThat(leer(get(RUTA_RESERVAS, idGala)).get(0).get("salida").asString()).isEqualTo(hora(16));

        int idJornada = leer(get("/api/logistica/eventos/{id}", idGala)).get("jornadas").get(0).get("id").asInt();
        JsonNode error = cuerpo(ejecutar(put(rutaJornadas + "/" + idJornada), jornada("MONTAJE", 14, 18))
                .andExpect(status().isConflict()));
        assertThat(error.get("conflictos").get(0).get("evento").asString()).isEqualTo("Almuerzo de prensa");
        assertThat(leer(get("/api/logistica/eventos/{id}", idGala)).get("jornadas").get(0).get("inicio").asString())
                .isEqualTo(hora(16));
        assertThat(leer(get(RUTA_RESERVAS, idGala)).get(0).get("salida").asString()).isEqualTo(hora(16));
    }

    @Test
    void reprogramarElEventoRecalculaLasReservasYRechazaLosConflictos() throws Exception {
        reservar(idGala, idPantalla).andExpect(status().isCreated());
        reservar(idAlmuerzo, idPantalla).andExpect(status().isCreated());
        String rutaAlmuerzo = "/api/comercial/proyectos/" + idProyecto + "/eventos/" + idAlmuerzo;

        ejecutar(put(rutaAlmuerzo), evento("Almuerzo de prensa", 11, 14)).andExpect(status().isOk());
        assertThat(leer(get(RUTA_RESERVAS, idAlmuerzo)).get(0).get("salida").asString()).isEqualTo(hora(11));

        ejecutar(put(rutaAlmuerzo), evento("Almuerzo de prensa", 18, 20)).andExpect(status().isConflict());
        assertThat(leer(get(RUTA_RESERVAS, idAlmuerzo)).get(0).get("retorno").asString()).isEqualTo(hora(14));
    }

    @Test
    void cancelarElEventoLiberaSusReservas() throws Exception {
        reservar(idGala, idPantalla, idProcesador).andExpect(status().isCreated());

        ejecutar(patch("/api/comercial/proyectos/" + idProyecto + "/eventos/" + idGala + "/estado"),
                Map.of("estado", "CANCELADO")).andExpect(status().isOk());

        assertThat(leer(get(RUTA_RESERVAS, idGala))).isEmpty();
        assertThat(jdbc.queryForList("select distinct motivo_liberacion from reservas_activo r "
                + "join planes_logisticos p on p.id_plan = r.id_plan where p.id_evento = ?", String.class, idGala))
                .containsExactly("Evento cancelado");
        assertThat(leer(get("/api/activos/{id}/reservas", idPantalla))).isEmpty();
    }

    @Test
    void unEquipoConReservasProximasNoSeDaDeBajaHastaLiberarlas() throws Exception {
        int idReserva = cuerpo(reservar(idGala, idPantalla).andExpect(status().isCreated())).get(0).get("id").asInt();
        String rutaBaja = "/api/activos/" + idPantalla + "/baja";

        JsonNode error = cuerpo(ejecutar(post(rutaBaja), Map.of("motivo", "Daño irreparable"))
                .andExpect(status().isConflict()));
        assertThat(error.get("conflictos")).singleElement()
                .satisfies(reserva -> assertThat(reserva.get("evento").asString()).isEqualTo("Cena de gala"));

        ejecutar(post("/api/logistica/eventos/" + idGala + "/reservas/" + idReserva + "/liberacion"),
                Map.of("motivo", "El equipo se dañó en bodega")).andExpect(status().isOk());
        ejecutar(post(rutaBaja), Map.of("motivo", "Daño irreparable")).andExpect(status().isOk());
    }

    @Test
    void quienSoloConsultaLaLogisticaNoPuedeReservar() throws Exception {
        jdbc.update("""
                delete from roles_permisos where id_rol = ?
                  and id_permiso = (select id_permiso from permisos where nombre = 'LOGISTICA_GESTIONAR')
                """, ID_ROL_GERENTE);
        crear("/api/usuarios", Map.of("nombreUsuario", "gerente.reservas", "nombres", "Usuario",
                "apellidoPaterno", "Reservas", "email", "gerente.reservas@visual.com.bo", "idRol", ID_ROL_GERENTE,
                "contrasenaTemporal", CONTRASENA_TEMPORAL));
        String tokenGerente = cambiarContrasena(iniciarSesion("gerente.reservas", CONTRASENA_TEMPORAL));

        mvc.perform(autorizado(post(RUTA_RESERVAS, idGala), tokenGerente).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idsActivos", List.of(idPantalla)))))
                .andExpect(status().isForbidden());
        mvc.perform(autorizado(get(RUTA_RESERVAS, idGala), tokenGerente)).andExpect(status().isOk());
    }

    private ResultActions reservar(int idEvento, Integer... idsActivos) throws Exception {
        return ejecutar(post(RUTA_RESERVAS, idEvento), Map.of("idsActivos", List.of(idsActivos)));
    }

    private int proyectoAceptado() throws Exception {
        int idServicio = crear("/api/comercial/catalogos/servicios", Map.of("nombre", "Pantalla de prueba reservas",
                "unidadMedida", "jornada", "precioReferencial", 6000, "activo", true));
        int idTipoCliente = idPorNombre(leer(get("/api/comercial/catalogos/tipos-cliente")), "Empresa privada");
        int idNit = idPorNombre(leer(get("/api/comercial/catalogos/tipos-documento")), "NIT");
        int idCliente = crear("/api/comercial/clientes", Map.of("idTipoCliente", idTipoCliente,
                "razonSocial", "Cliente de prueba reservas", "documentos",
                List.of(Map.of("idTipoDocumento", idNit, "numero", "880011077"))));
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("idCliente", idCliente);
        cotizacion.put("titulo", "Gala con pantalla");
        cotizacion.put("fechaEmision", LocalDate.now().toString());
        cotizacion.put("fechaValidez", LocalDate.now().plusDays(5).toString());
        cotizacion.put("fechaEventoInicio", dia.toString());
        cotizacion.put("fechaEventoFin", dia.toString());
        cotizacion.put("lineas", List.of(Map.of("idServicio", idServicio, "cantidad", 1, "precioUnitario", 6000,
                "descuento", 0)));
        int idCotizacion = crear("/api/comercial/cotizaciones", cotizacion);
        return cuerpo(mvc.perform(autorizado(post("/api/comercial/cotizaciones/{id}/aceptacion", idCotizacion), token))
                .andExpect(status().isOk())).get("idProyecto").asInt();
    }

    private JsonNode registrarEquipos(int cantidad) throws Exception {
        int idBodega = leer(get("/api/activos/catalogos/bodegas")).get(0).get("id").asInt();
        int idCategoria = leer(get("/api/activos/catalogos/categorias")).get(0).get("id").asInt();
        int idFabricante = crear("/api/activos/catalogos/fabricantes", Map.of("nombre", "Fabricante reservas"));
        Map<String, Object> modelo = new HashMap<>(Map.of("idFabricante", idFabricante, "idCategoria", idCategoria,
                "nombre", "Pantalla reservas", "tipoControl", "UNIDAD"));
        modelo.put("prefijoCodigo", "RSV");
        int idModelo = crear("/api/activos/modelos", modelo);
        return cuerpo(ejecutar(post("/api/activos"), Map.of("idModelo", idModelo, "idBodega", idBodega,
                "cantidad", cantidad)).andExpect(status().isCreated()));
    }

    private int crearEvento(String nombre, int inicio, int fin) throws Exception {
        return crear("/api/comercial/proyectos/" + idProyecto + "/eventos", evento(nombre, inicio, fin));
    }

    private Map<String, Object> evento(String nombre, int inicio, int fin) {
        return Map.of("idLugar", idLugar, "nombre", nombre, "fechaInicio", hora(inicio), "fechaFin", hora(fin));
    }

    private Map<String, Object> jornada(String tipo, int inicio, int fin) {
        return Map.of("tipo", tipo, "inicio", hora(inicio), "fin", hora(fin), "descripcion", "Armado de pantalla");
    }

    private String hora(int hora) {
        return dia + String.format("T%02d:00:00", hora);
    }

    private static List<Integer> ids(JsonNode elementos) {
        return elementos.valueStream().map(elemento -> elemento.get("id").asInt()).toList();
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
