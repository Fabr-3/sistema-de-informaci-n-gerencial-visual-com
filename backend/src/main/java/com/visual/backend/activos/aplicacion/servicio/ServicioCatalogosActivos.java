package com.visual.backend.activos.aplicacion.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.activos.aplicacion.dto.ComandoBodega;
import com.visual.backend.activos.aplicacion.dto.ComandoCatalogo;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/** Mantenimiento de los catalogos del modulo: categorias, fabricantes y bodegas. */
@Service
public class ServicioCatalogosActivos {

    private static final String ENTIDAD_CATEGORIAS = "categorias_activo";
    private static final String ENTIDAD_FABRICANTES = "fabricantes";
    private static final String ENTIDAD_BODEGAS = "bodegas";
    private static final String CAMPO_NOMBRE = "nombre";

    private final RepositorioCatalogosActivos repositorio;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;

    public ServicioCatalogosActivos(RepositorioCatalogosActivos repositorio, ServicioAuditoria auditoria,
                                    ContextoSolicitud contexto) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return repositorio.listarCategorias();
    }

    @Transactional
    public Categoria guardarCategoria(Integer id, ComandoCatalogo comando) {
        String nombre = comando.nombre().trim();
        Categoria anterior = id == null ? null : repositorio.buscarCategoria(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la categoría", id));
        if (repositorio.existeCategoria(nombre, id)) {
            throw new DatoDuplicadoExcepcion("La categoría " + nombre + " ya existe");
        }
        Categoria guardada = repositorio.guardarCategoria(new Categoria(id, nombre));
        auditar(id, ENTIDAD_CATEGORIAS, guardada.id(), anterior == null ? null : anterior.nombre(), nombre);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Fabricante> listarFabricantes() {
        return repositorio.listarFabricantes();
    }

    @Transactional
    public Fabricante guardarFabricante(Integer id, ComandoCatalogo comando) {
        String nombre = comando.nombre().trim();
        Fabricante anterior = id == null ? null : repositorio.buscarFabricante(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el fabricante", id));
        if (repositorio.existeFabricante(nombre, id)) {
            throw new DatoDuplicadoExcepcion("El fabricante " + nombre + " ya existe");
        }
        Fabricante guardado = repositorio.guardarFabricante(new Fabricante(id, nombre));
        auditar(id, ENTIDAD_FABRICANTES, guardado.id(), anterior == null ? null : anterior.nombre(), nombre);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Bodega> listarBodegas() {
        return repositorio.listarBodegas();
    }

    @Transactional
    public Bodega guardarBodega(Integer id, ComandoBodega comando) {
        String nombre = comando.nombre().trim();
        Bodega anterior = id == null ? null : repositorio.buscarBodega(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la bodega", id));
        if (repositorio.existeBodega(nombre, id)) {
            throw new DatoDuplicadoExcepcion("La bodega " + nombre + " ya existe");
        }
        String direccion = comando.direccion() == null || comando.direccion().isBlank()
                ? null : comando.direccion().trim();
        Bodega guardada = repositorio.guardarBodega(new Bodega(id, nombre, direccion, comando.activa()),
                contexto.idUsuarioActual());
        auditoria.registrarOperacion(id == null ? AccionAuditoria.CREAR : AccionAuditoria.ACTUALIZAR,
                ENTIDAD_BODEGAS, guardada.id(), anterior == null ? null : mapaBodega(anterior),
                mapaBodega(guardada), null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<TipoMantenimiento> listarTiposMantenimiento() {
        return repositorio.listarTiposMantenimiento();
    }

    private void auditar(Integer idOriginal, String entidad, Integer idEntidad, String nombreAnterior,
                         String nombreNuevo) {
        auditoria.registrarOperacion(idOriginal == null ? AccionAuditoria.CREAR : AccionAuditoria.ACTUALIZAR,
                entidad, idEntidad, nombreAnterior == null ? null : Map.of(CAMPO_NOMBRE, nombreAnterior),
                Map.of(CAMPO_NOMBRE, nombreNuevo), null);
    }

    private static Map<String, Object> mapaBodega(Bodega bodega) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put(CAMPO_NOMBRE, bodega.nombre());
        mapa.put("direccion", bodega.direccion());
        mapa.put("activa", bodega.activa());
        return mapa;
    }
}
