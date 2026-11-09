package com.visual.backend.activos.dominio.puerto;

import java.time.LocalDateTime;
import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

public interface RepositorioRegistrosUso {

    /** Registros vigentes (no anulados) del activo, del mas reciente al mas antiguo. */
    Pagina<RegistroUso> listarPorActivo(Integer idActivo, SolicitudPagina solicitud);

    Optional<RegistroUso> buscarPorId(Integer id);

    /** Indica si el periodo se superpone con otro uso vigente del mismo activo. */
    boolean existeSuperposicion(Integer idActivo, LocalDateTime inicio, LocalDateTime fin);

    RegistroUso guardar(RegistroUso registro);

    void anular(Integer id, Integer actor, LocalDateTime ahora);
}
