package com.visual.backend.comercial.aplicacion.servicio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.aplicacion.dto.ComandoContrato;
import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.puerto.AlmacenDocumentos;
import com.visual.backend.comercial.dominio.puerto.GeneradorDocumentos;
import com.visual.backend.comercial.dominio.puerto.RepositorioClientes;
import com.visual.backend.comercial.dominio.puerto.RepositorioContratos;
import com.visual.backend.comercial.dominio.puerto.RepositorioCotizaciones;
import com.visual.backend.comercial.dominio.puerto.RepositorioEventos;
import com.visual.backend.comercial.dominio.puerto.RepositorioProyectos;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Contratos de los proyectos y su documento PDF con la plantilla estandar. */
@Service
public class ServicioContratos {

    public static final String ENTIDAD = "contratos";
    /** Numeracion anual segun el ano de firma: CTR-2026-0001. */
    private static final String FORMATO_NUMERO = "CTR-%d-%04d";
    private static final String FORMATO_PREFIJO = "CTR-%d-";
    /** Tamano maximo del contrato firmado; coincide con spring.servlet.multipart.max-file-size. */
    public static final int TAMANO_MAXIMO_FIRMADO = 10 * 1024 * 1024;
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String SUFIJO_FIRMADO = "-firmado.pdf";
    private static final String DETALLE_FIRMADO = "Contrato firmado adjunto";

    private final RepositorioContratos repositorio;
    private final RepositorioProyectos proyectos;
    private final RepositorioCotizaciones cotizaciones;
    private final RepositorioClientes clientes;
    private final RepositorioEventos eventos;
    private final GeneradorDocumentos documentos;
    private final AlmacenDocumentos almacen;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioContratos(RepositorioContratos repositorio, RepositorioProyectos proyectos,
                             RepositorioCotizaciones cotizaciones, RepositorioClientes clientes,
                             RepositorioEventos eventos, GeneradorDocumentos documentos, AlmacenDocumentos almacen,
                             ServicioAuditoria auditoria,
                             ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.proyectos = proyectos;
        this.cotizaciones = cotizaciones;
        this.clientes = clientes;
        this.eventos = eventos;
        this.documentos = documentos;
        this.almacen = almacen;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional
    public Contrato crear(Integer idProyecto, ComandoContrato comando) {
        Proyecto proyecto = proyectos.obtener(idProyecto);
        int anio = comando.fechaFirma().getYear();
        int correlativo = repositorio.reservarCorrelativo(String.format(FORMATO_PREFIJO, anio)) + 1;
        Contrato contrato = repositorio.guardar(Contrato.nuevo(proyecto, String.format(FORMATO_NUMERO, anio,
                correlativo), comando.fechaFirma(), comando.terminos(), contexto.idUsuarioActual(),
                LocalDateTime.now(reloj)));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, contrato.getId(), null, mapa(contrato), null);
        return contrato;
    }

    @Transactional
    public Contrato actualizar(Integer idProyecto, Integer idContrato, ComandoContrato comando) {
        Contrato contrato = buscarDelProyecto(idProyecto, idContrato);
        Map<String, Object> anterior = mapa(contrato);
        contrato.actualizar(comando.fechaFirma(), comando.terminos(), contexto.idUsuarioActual(),
                LocalDateTime.now(reloj));
        Contrato guardado = repositorio.guardar(contrato);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idContrato, anterior, mapa(guardado), null);
        return guardado;
    }

    @Transactional(readOnly = true)
    public DocumentoGenerado pdf(Integer idProyecto, Integer idContrato) {
        Contrato contrato = buscarDelProyecto(idProyecto, idContrato);
        Proyecto proyecto = proyectos.obtener(idProyecto);
        Cotizacion cotizacion = cotizaciones.obtener(proyecto.getIdCotizacion());
        byte[] contenido = documentos.contrato(contrato, proyecto, cotizacion,
                clientes.obtener(cotizacion.getCliente().id()), eventos.listarPorProyecto(idProyecto));
        return new DocumentoGenerado(contrato.getNumero() + ".pdf", contenido);
    }

    /** Adjunta (o reemplaza) el escaneo del contrato firmado, que debe ser un PDF. */
    @Transactional
    public Contrato adjuntarFirmado(Integer idProyecto, Integer idContrato, byte[] contenido) {
        validarPdf(contenido);
        Contrato contrato = buscarDelProyecto(idProyecto, idContrato);
        Map<String, Object> anterior = mapa(contrato);
        contrato.adjuntarFirmado(contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        Contrato guardado = repositorio.guardar(contrato);
        almacen.guardar(guardado.getArchivoFirmado(), contenido);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, idContrato, anterior, mapa(guardado),
                DETALLE_FIRMADO);
        return guardado;
    }

    @Transactional(readOnly = true)
    public DocumentoGenerado firmado(Integer idProyecto, Integer idContrato) {
        Contrato contrato = buscarDelProyecto(idProyecto, idContrato);
        byte[] contenido = contrato.tieneArchivoFirmado()
                ? almacen.leer(contrato.getArchivoFirmado()).orElse(null) : null;
        if (contenido == null) {
            throw new RecursoNoEncontradoExcepcion("el contrato firmado", contrato.getNumero());
        }
        return new DocumentoGenerado(contrato.getNumero() + SUFIJO_FIRMADO, contenido);
    }

    private static void validarPdf(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ReglaNegocioExcepcion("Seleccione el archivo del contrato firmado");
        }
        if (contenido.length > TAMANO_MAXIMO_FIRMADO) {
            throw new ReglaNegocioExcepcion("El contrato firmado no puede superar los 10 MB");
        }
        if (contenido.length < FIRMA_PDF.length
                || !Arrays.equals(Arrays.copyOf(contenido, FIRMA_PDF.length), FIRMA_PDF)) {
            throw new ReglaNegocioExcepcion("El contrato firmado debe ser un archivo PDF");
        }
    }

    private Contrato buscarDelProyecto(Integer idProyecto, Integer idContrato) {
        Contrato contrato = repositorio.obtener(idContrato);
        if (!contrato.getIdProyecto().equals(idProyecto)) {
            throw new RecursoNoEncontradoExcepcion("el contrato", idContrato);
        }
        return contrato;
    }

    private static Map<String, Object> mapa(Contrato contrato) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("numero", contrato.getNumero());
        mapa.put("proyecto", contrato.getIdProyecto());
        mapa.put("fechaFirma", contrato.getFechaFirma().toString());
        mapa.put("terminos", contrato.getTerminos());
        mapa.put("archivoFirmado", contrato.getArchivoFirmado());
        return mapa;
    }
}
