package com.visual.backend.activos.aplicacion.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.activos.aplicacion.dto.ComandoModelo;
import com.visual.backend.activos.aplicacion.dto.VistaModelo;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Especificacion;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.TipoControl;
import com.visual.backend.activos.dominio.puerto.RepositorioCatalogosActivos;
import com.visual.backend.activos.dominio.puerto.RepositorioModelos;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Modelos de equipo con sus especificaciones tecnicas. */
@Service
public class ServicioModelos {

    public static final String ENTIDAD = "modelos_activo";

    private final RepositorioModelos repositorio;
    private final RepositorioCatalogosActivos catalogos;
    private final ServicioAuditoria auditoria;

    public ServicioModelos(RepositorioModelos repositorio, RepositorioCatalogosActivos catalogos,
                           ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.catalogos = catalogos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<VistaModelo> listar(TipoControl tipoControl) {
        Map<Integer, Long> inventario = repositorio.contarInventarioPorModelo();
        return repositorio.listar(tipoControl).stream()
                .map(modelo -> VistaModelo.de(modelo, inventario.getOrDefault(modelo.id(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public VistaModelo obtener(Integer id) {
        return vista(buscar(id));
    }

    @Transactional
    public VistaModelo crear(ComandoModelo comando) {
        ModeloActivo modelo = construir(null, comando);
        verificarDisponibilidad(modelo, null);
        ModeloActivo creado = repositorio.guardar(modelo);
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null, mapa(creado), null);
        return vista(creado);
    }

    /**
     * El tipo de control no cambia si el modelo ya tiene inventario: los
     * activos por unidad y las existencias por lote no son convertibles.
     */
    @Transactional
    public VistaModelo actualizar(Integer id, ComandoModelo comando) {
        ModeloActivo anterior = buscar(id);
        ModeloActivo modelo = construir(id, comando);
        verificarDisponibilidad(modelo, id);
        verificarCambiosConInventario(anterior, modelo);
        ModeloActivo actualizado = repositorio.guardar(modelo);
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, mapa(anterior), mapa(actualizado),
                null);
        return vista(actualizado);
    }

    /**
     * Con inventario registrado no cambia el tipo de control (no son
     * convertibles) ni un prefijo ya definido (los codigos existentes quedarian
     * con otro prefijo). Si el modelo no tenia prefijo, se puede definir.
     */
    private void verificarCambiosConInventario(ModeloActivo anterior, ModeloActivo modelo) {
        boolean cambiaTipo = modelo.tipoControl() != anterior.tipoControl();
        boolean cambiaPrefijo = anterior.prefijoCodigo() != null
                && !Objects.equals(anterior.prefijoCodigo(), modelo.prefijoCodigo());
        if ((cambiaTipo || cambiaPrefijo) && repositorio.tieneInventario(anterior.id())) {
            throw new ReglaNegocioExcepcion(
                    "No se puede cambiar el tipo de control ni el prefijo de un modelo con inventario registrado");
        }
    }

    private VistaModelo vista(ModeloActivo modelo) {
        return VistaModelo.de(modelo, repositorio.contarInventarioPorModelo().getOrDefault(modelo.id(), 0L));
    }

    private ModeloActivo construir(Integer id, ComandoModelo comando) {
        Fabricante fabricante = catalogos.buscarFabricante(comando.idFabricante())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el fabricante", comando.idFabricante()));
        Categoria categoria = catalogos.buscarCategoria(comando.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la categoría", comando.idCategoria()));
        List<Especificacion> especificaciones = comando.especificaciones() == null ? List.of()
                : comando.especificaciones().stream()
                        .map(especificacion -> new Especificacion(especificacion.caracteristica().trim(),
                                especificacion.valor().trim()))
                        .toList();
        ModeloActivo modelo = new ModeloActivo(id, comando.nombre(), fabricante, categoria, comando.tipoControl(),
                comando.horasMantenimiento(), comando.prefijoCodigo(), especificaciones);
        modelo.validarPrefijoDefinido();
        return modelo;
    }

    private void verificarDisponibilidad(ModeloActivo modelo, Integer idExcluido) {
        if (repositorio.existe(modelo.fabricante().id(), modelo.nombre(), idExcluido)) {
            throw new DatoDuplicadoExcepcion("El modelo " + modelo.nombreCompleto() + " ya existe");
        }
        if (modelo.prefijoCodigo() != null && repositorio.existePrefijo(modelo.prefijoCodigo(), idExcluido)) {
            throw new DatoDuplicadoExcepcion("El prefijo " + modelo.prefijoCodigo() + " ya lo usa otro modelo");
        }
    }

    private ModeloActivo buscar(Integer id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el modelo", id));
    }

    private static Map<String, Object> mapa(ModeloActivo modelo) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("modelo", modelo.nombreCompleto());
        mapa.put("categoria", modelo.categoria().nombre());
        mapa.put("tipoControl", modelo.tipoControl().name());
        mapa.put("horasMantenimiento", modelo.horasMantenimiento());
        mapa.put("prefijoCodigo", modelo.prefijoCodigo());
        mapa.put("especificaciones", modelo.especificaciones().stream()
                .map(especificacion -> especificacion.caracteristica() + ": " + especificacion.valor())
                .toList());
        return mapa;
    }
}
