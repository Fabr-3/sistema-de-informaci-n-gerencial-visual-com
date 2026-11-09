package com.visual.backend.comercial.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoCotizacion;
import com.visual.backend.comercial.aplicacion.dto.VistaCotizacion;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.DatosCotizacion;
import com.visual.backend.comercial.dominio.modelo.Filtros;
import com.visual.backend.comercial.dominio.modelo.LineaCotizacion;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.modelo.Servicio;
import com.visual.backend.comercial.dominio.puerto.GeneradorDocumentos;
import com.visual.backend.comercial.dominio.puerto.RepositorioCatalogosComerciales;
import com.visual.backend.comercial.dominio.puerto.RepositorioClientes;
import com.visual.backend.comercial.dominio.puerto.RepositorioCotizaciones;
import com.visual.backend.comercial.dominio.puerto.RepositorioProyectos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Ciclo de las cotizaciones: borrador, aceptacion o rechazo por el gerente y
 * vencimiento automatico. Al aceptarse se crea el proyecto correspondiente.
 */
@Service
public class ServicioCotizaciones {

    public static final String ENTIDAD = "cotizaciones";
    /** Numeracion anual: COT-2026-0001. */
    private static final String FORMATO_NUMERO = "COT-%d-%04d";
    private static final String FORMATO_PREFIJO = "COT-%d-";
    private static final String DETALLE_VENCIMIENTO = "Vencimiento automático por fecha de validez";

    private final RepositorioCotizaciones repositorio;
    private final RepositorioClientes clientes;
    private final RepositorioCatalogosComerciales catalogos;
    private final RepositorioProyectos proyectos;
    private final GeneradorDocumentos documentos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioCotizaciones(RepositorioCotizaciones repositorio, RepositorioClientes clientes,
                                RepositorioCatalogosComerciales catalogos, RepositorioProyectos proyectos,
                                GeneradorDocumentos documentos, ServicioAuditoria auditoria,
                                ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.catalogos = catalogos;
        this.proyectos = proyectos;
        this.documentos = documentos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaCotizacion> listar(Filtros.Cotizaciones filtro, SolicitudPagina solicitud) {
        Pagina<Cotizacion> pagina = repositorio.buscar(filtro, solicitud);
        Map<Integer, Integer> proyectosGenerados = proyectos.idsPorCotizacion(
                pagina.contenido().stream().map(Cotizacion::getId).toList());
        return pagina.mapear(cotizacion -> VistaCotizacion.de(cotizacion, proyectosGenerados.get(cotizacion.getId())));
    }

    @Transactional(readOnly = true)
    public VistaCotizacion obtener(Integer id) {
        return vista(repositorio.obtener(id));
    }

    @Transactional
    public VistaCotizacion crear(ComandoCotizacion comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int anio = comando.fechaEmision().getYear();
        int correlativo = repositorio.reservarCorrelativo(String.format(FORMATO_PREFIJO, anio)) + 1;
        Cotizacion cotizacion = Cotizacion.nueva(String.format(FORMATO_NUMERO, anio, correlativo),
                datos(comando, Set.of()), contexto.idUsuarioActual(), ahora);
        VistaCotizacion creada = vista(repositorio.guardar(cotizacion));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creada.id(), null, creada.comoMapaAuditoria(),
                null);
        return creada;
    }

    @Transactional
    public VistaCotizacion actualizar(Integer id, ComandoCotizacion comando) {
        Cotizacion cotizacion = repositorio.obtener(id);
        VistaCotizacion anterior = vista(cotizacion);
        Set<Integer> serviciosActuales = cotizacion.getLineas().stream()
                .map(LineaCotizacion::idServicio).collect(Collectors.toSet());
        cotizacion.actualizar(datos(comando, serviciosActuales), contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaCotizacion actualizada = vista(repositorio.guardar(cotizacion));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizada.comoMapaAuditoria(), null);
        return actualizada;
    }

    @Transactional
    public void eliminar(Integer id) {
        Cotizacion cotizacion = repositorio.obtener(id);
        VistaCotizacion anterior = vista(cotizacion);
        cotizacion.eliminar(contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        repositorio.guardar(cotizacion);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, id, anterior.comoMapaAuditoria(), null, null);
    }

    /** Aceptacion por el gerente: genera el proyecto con el titulo y el periodo cotizados. */
    @Transactional
    public VistaCotizacion aceptar(Integer id) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Integer actor = contexto.idUsuarioActual();
        Cotizacion cotizacion = repositorio.obtener(id);
        VistaCotizacion anterior = vista(cotizacion);

        cotizacion.aceptar(actor, ahora);
        Cotizacion aceptada = repositorio.guardar(cotizacion);
        Proyecto proyecto = proyectos.guardar(Proyecto.desdeCotizacion(aceptada, actor, ahora));

        VistaCotizacion vista = VistaCotizacion.de(aceptada, proyecto.getId());
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                vista.comoMapaAuditoria(), "Cotización aceptada; proyecto generado");
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ServicioProyectos.ENTIDAD, proyecto.getId(), null,
                Map.of("nombre", proyecto.getNombre(), "cotizacion", aceptada.getNumero()), null);
        return vista;
    }

    @Transactional
    public VistaCotizacion rechazar(Integer id, String motivo) {
        Cotizacion cotizacion = repositorio.obtener(id);
        VistaCotizacion anterior = vista(cotizacion);
        cotizacion.rechazar(motivo, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaCotizacion rechazada = vista(repositorio.guardar(cotizacion));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                rechazada.comoMapaAuditoria(), "Cotización rechazada");
        return rechazada;
    }

    /** Marca vencidos los borradores cuya validez ya paso. Devuelve cuantos vencieron. */
    @Transactional
    public int vencerCaducadas() {
        LocalDate hoy = LocalDate.now(reloj);
        List<Cotizacion> caducadas = repositorio.buscarBorradoresCaducados(hoy);
        for (Cotizacion cotizacion : caducadas) {
            if (cotizacion.vencerSiCorresponde(hoy)) {
                repositorio.guardar(cotizacion);
                auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, cotizacion.getId(), null,
                        Map.of("estado", cotizacion.getEstado().name()), DETALLE_VENCIMIENTO);
            }
        }
        return caducadas.size();
    }

    /** Documento PDF de la cotizacion para entregar al cliente. */
    @Transactional(readOnly = true)
    public DocumentoGenerado pdf(Integer id) {
        Cotizacion cotizacion = repositorio.obtener(id);
        byte[] contenido = documentos.cotizacion(cotizacion, clientes.obtener(cotizacion.getCliente().id()));
        return new DocumentoGenerado(cotizacion.getNumero() + ".pdf", contenido);
    }

    private VistaCotizacion vista(Cotizacion cotizacion) {
        Integer idProyecto = proyectos.buscarPorCotizacion(cotizacion.getId()).map(Proyecto::getId).orElse(null);
        return VistaCotizacion.de(cotizacion, idProyecto);
    }

    /**
     * Arma el contenido con los datos del catalogo. Solo se admiten servicios
     * activos, salvo los que la cotizacion ya tenia.
     */
    private DatosCotizacion datos(ComandoCotizacion comando, Set<Integer> serviciosPermitidos) {
        Map<Integer, Servicio> servicios = catalogos.buscarServicios(
                        comando.lineas().stream().map(ComandoCotizacion.Linea::idServicio).toList())
                .stream().collect(Collectors.toMap(Servicio::id, Function.identity()));
        List<LineaCotizacion> lineas = comando.lineas().stream().map(linea -> {
            Servicio servicio = servicios.get(linea.idServicio());
            if (servicio == null) {
                throw new RecursoNoEncontradoExcepcion("el servicio", linea.idServicio());
            }
            if (!servicio.activo() && !serviciosPermitidos.contains(servicio.id())) {
                throw new ReglaNegocioExcepcion("El servicio " + servicio.nombre() + " está inactivo");
            }
            return new LineaCotizacion(servicio.id(), servicio.nombre(), servicio.unidadMedida(), linea.cantidad(),
                    linea.precioUnitario(), linea.descuento());
        }).toList();
        return new DatosCotizacion(clientes.obtener(comando.idCliente()).resumen(), comando.titulo(),
                comando.fechaEmision(), comando.fechaValidez(), comando.fechaEventoInicio(), comando.fechaEventoFin(),
                comando.condiciones(), lineas);
    }
}
