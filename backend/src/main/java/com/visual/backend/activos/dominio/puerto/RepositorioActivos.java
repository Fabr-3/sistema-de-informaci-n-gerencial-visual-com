package com.visual.backend.activos.dominio.puerto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.FiltroActivos;
import com.visual.backend.activos.dominio.modelo.UsoActivo;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;

/** Persistencia de activos por unidad. Las consultas excluyen los eliminados. */
public interface RepositorioActivos {

    Optional<Activo> buscarPorId(Integer id);

    /** Activo vigente o excepcion de recurso no encontrado (reutilizado por los casos de uso). */
    default Activo obtenerVigente(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el activo", id));
    }

    boolean existeCodigo(String codigoInventario);

    boolean existeSerie(Integer idModelo, String numeroSerie, Integer idExcluido);

    /**
     * Bloquea la generacion de codigos del prefijo hasta el fin de la
     * transaccion y devuelve el mayor correlativo usado (0 si no hay).
     */
    int reservarCorrelativo(String prefijoCodigo);

    /**
     * Busqueda paginada. Con soloAlertas se filtran los activos cuyas horas
     * desde el ultimo mantenimiento alcanzan el porcentaje indicado del intervalo.
     */
    Pagina<Activo> buscar(FiltroActivos filtro, int porcentajeAlerta, SolicitudPagina solicitud);

    /** Activos operativos (no dados de baja) que alcanzaron el umbral de mantenimiento. */
    List<Activo> buscarConAlertaMantenimiento(int porcentajeAlerta);

    UsoActivo obtenerUso(Integer idActivo);

    Map<Integer, UsoActivo> obtenerUso(List<Integer> idsActivos);

    /** Indica si tiene historial de uso, mantenimientos o modulos registrados. */
    boolean tieneHistorial(Integer idActivo);

    Activo guardar(Activo activo);
}
