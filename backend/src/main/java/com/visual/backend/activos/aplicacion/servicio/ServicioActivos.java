package com.visual.backend.activos.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.activos.aplicacion.dto.ComandoActualizarActivo;
import com.visual.backend.activos.aplicacion.dto.ComandoRegistroActivos;
import com.visual.backend.activos.aplicacion.dto.VistaActivo;
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.DatosActivo;
import com.visual.backend.activos.dominio.modelo.FiltroActivos;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.UsoActivo;
import com.visual.backend.activos.dominio.puerto.RepositorioActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioModelos;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Registro y control de los activos por unidad. */
@Service
public class ServicioActivos {

    public static final String ENTIDAD = "activos";

    private final RepositorioActivos repositorio;
    private final RepositorioModelos modelos;
    private final RepositorioCatalogosActivos catalogos;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final ParametrosActivos parametros;
    private final Clock reloj;

    public ServicioActivos(RepositorioActivos repositorio, RepositorioModelos modelos,
                           RepositorioCatalogosActivos catalogos, ServicioAuditoria auditoria,
                           ContextoSolicitud contexto, ParametrosActivos parametros, Clock reloj) {
        this.repositorio = repositorio;
        this.modelos = modelos;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaActivo> listar(FiltroActivos filtro, SolicitudPagina solicitud) {
        Pagina<Activo> pagina = repositorio.buscar(filtro, parametros.porcentajeAlerta(), solicitud);
        return pagina.mapear(conUso(pagina.contenido()));
    }

    /** Activos que alcanzaron el umbral de su mantenimiento preventivo, del mas urgente al menos. */
    @Transactional(readOnly = true)
    public List<VistaActivo> listarAlertasMantenimiento() {
        List<Activo> activos = repositorio.buscarConAlertaMantenimiento(parametros.porcentajeAlerta());
        return activos.stream()
                .map(conUso(activos)::apply)
                .sorted((a, b) -> b.nivelAlerta().compareTo(a.nivelAlerta()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VistaActivo obtener(Integer id) {
        return vista(buscarActivo(id));
    }

    /**
     * Registra una o varias unidades del mismo modelo, cada una con su propio
     * codigo de inventario (PREFIJO-NNNN correlativo, o manual si es una sola).
     */
    @Transactional
    public List<VistaActivo> registrar(ComandoRegistroActivos comando) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        ModeloActivo modelo = buscarModelo(comando.idModelo());
        Bodega bodega = buscarBodega(comando.idBodega());
        List<String> series = seriesPorUnidad(comando, modelo);
        List<String> codigos = codigosPorUnidad(comando, modelo);
        String detalle = comando.cantidad() > 1 ? "Registro de " + comando.cantidad() + " unidades" : null;

        List<VistaActivo> registrados = new ArrayList<>();
        for (int indice = 0; indice < comando.cantidad(); indice++) {
            DatosActivo datos = new DatosActivo(codigos.get(indice), series.get(indice), comando.fechaAdquisicion(),
                    comando.costoUnitario(), comando.observaciones());
            VistaActivo creado = vista(repositorio.guardar(
                    Activo.nuevo(datos, modelo, bodega, contexto.idUsuarioActual(), ahora)));
            auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null,
                    creado.comoMapaAuditoria(), detalle);
            registrados.add(creado);
        }
        return registrados;
    }

    @Transactional
    public VistaActivo actualizar(Integer id, ComandoActualizarActivo comando) {
        Activo activo = buscarActivo(id);
        VistaActivo anterior = vista(activo);
        ModeloActivo modelo = buscarModelo(comando.idModelo());
        verificarSerieDisponible(modelo, comando.numeroSerie(), id);

        DatosActivo datos = new DatosActivo(activo.getCodigoInventario(), comando.numeroSerie(),
                comando.fechaAdquisicion(), comando.costoAdquisicion(), comando.observaciones());
        activo.actualizar(datos, modelo, buscarBodega(comando.idBodega()), contexto.idUsuarioActual(),
                LocalDateTime.now(reloj));
        VistaActivo actualizado = vista(repositorio.guardar(activo));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    @Transactional
    public VistaActivo darDeBaja(Integer id, String motivo) {
        Activo activo = buscarActivo(id);
        VistaActivo anterior = vista(activo);
        activo.darDeBaja(motivo, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaActivo actualizado = vista(repositorio.guardar(activo));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), "Baja del activo");
        return actualizado;
    }

    /** Marca el equipo como vendido; lo invoca el modulo financiero al registrar la venta. */
    @Transactional
    public VistaActivo vender(Integer id, String referencia) {
        Activo activo = buscarActivo(id);
        VistaActivo anterior = vista(activo);
        activo.vender(referencia, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaActivo actualizado = vista(repositorio.guardar(activo));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), "Venta del activo: " + referencia);
        return actualizado;
    }

    /** Devuelve el equipo al inventario al anular su venta. */
    @Transactional
    public VistaActivo revertirVenta(Integer id, String referencia) {
        Activo activo = buscarActivo(id);
        VistaActivo anterior = vista(activo);
        activo.revertirVenta(referencia, contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        VistaActivo actualizado = vista(repositorio.guardar(activo));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), "Venta anulada: " + referencia);
        return actualizado;
    }

    /**
     * Solo para corregir registros erroneos: un activo con historial no se
     * elimina, se da de baja para conservar su trazabilidad.
     */
    @Transactional
    public void eliminar(Integer id) {
        Activo activo = buscarActivo(id);
        if (repositorio.tieneHistorial(id)) {
            throw new ReglaNegocioExcepcion(
                    "El activo tiene historial de uso, mantenimientos o módulos: debe darse de baja");
        }
        VistaActivo anterior = vista(activo);
        activo.eliminar(contexto.idUsuarioActual(), LocalDateTime.now(reloj));
        repositorio.guardar(activo);
        auditoria.registrarOperacion(AccionAuditoria.ELIMINAR, ENTIDAD, id, anterior.comoMapaAuditoria(), null,
                null);
    }

    private Activo buscarActivo(Integer id) {
        return repositorio.obtenerVigente(id);
    }

    private VistaActivo vista(Activo activo) {
        return VistaActivo.de(activo, repositorio.obtenerUso(activo.getId()), parametros.porcentajeAlerta());
    }

    /** Consulta las horas de todos los activos de una vez (evita una consulta por fila). */
    private Function<Activo, VistaActivo> conUso(List<Activo> activos) {
        Map<Integer, UsoActivo> usos = repositorio.obtenerUso(activos.stream().map(Activo::getId).toList());
        return activo -> VistaActivo.de(activo, usos.getOrDefault(activo.getId(), UsoActivo.SIN_USO),
                parametros.porcentajeAlerta());
    }

    private void verificarSerieDisponible(ModeloActivo modelo, String numeroSerie, Integer idExcluido) {
        if (numeroSerie != null && !numeroSerie.isBlank()
                && repositorio.existeSerie(modelo.id(), numeroSerie.trim(), idExcluido)) {
            throw new DatoDuplicadoExcepcion("Ya existe un " + modelo.nombreCompleto() + " con la serie "
                    + numeroSerie.trim());
        }
    }

    private ModeloActivo buscarModelo(Integer id) {
        return modelos.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el modelo", id));
    }

    private Bodega buscarBodega(Integer id) {
        return catalogos.buscarBodega(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la bodega", id));
    }

    /** Codigo manual (solo para una unidad) o correlativos a partir del prefijo del modelo. */
    private List<String> codigosPorUnidad(ComandoRegistroActivos comando, ModeloActivo modelo) {
        String manual = Activo.normalizarCodigo(comando.codigoInventario());
        if (manual != null && !manual.isEmpty()) {
            if (comando.cantidad() > 1) {
                throw new ReglaNegocioExcepcion("El código manual solo aplica al registrar una unidad");
            }
            if (repositorio.existeCodigo(manual)) {
                throw new DatoDuplicadoExcepcion("El código de inventario " + manual + " ya está registrado");
            }
            return List.of(manual);
        }
        modelo.validarPrefijoDefinido();
        int ultimo = repositorio.reservarCorrelativo(modelo.prefijoCodigo());
        List<String> codigos = new ArrayList<>();
        for (int correlativo = ultimo + 1; correlativo <= ultimo + comando.cantidad(); correlativo++) {
            codigos.add(modelo.codigoInventario(correlativo));
        }
        return codigos;
    }

    /** Una serie por unidad (o ninguna); sin repetidos en el lote ni ya registrados para el modelo. */
    private List<String> seriesPorUnidad(ComandoRegistroActivos comando, ModeloActivo modelo) {
        List<String> recibidas = comando.numerosSerie() == null ? List.of() : comando.numerosSerie().stream()
                .map(serie -> serie == null || serie.isBlank() ? null : serie.trim())
                .toList();
        if (recibidas.stream().allMatch(Objects::isNull)) {
            return Collections.nCopies(comando.cantidad(), null);
        }
        if (recibidas.size() != comando.cantidad()) {
            throw new ReglaNegocioExcepcion("Indique un número de serie por unidad o deje las series vacías");
        }
        HashSet<String> unicas = new HashSet<>();
        for (String serie : recibidas) {
            if (serie != null && !unicas.add(serie.toUpperCase(Locale.ROOT))) {
                throw new DatoDuplicadoExcepcion("El número de serie " + serie + " está repetido");
            }
            verificarSerieDisponible(modelo, serie, null);
        }
        return recibidas;
    }
}
