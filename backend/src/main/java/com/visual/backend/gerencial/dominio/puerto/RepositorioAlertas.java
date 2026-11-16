package com.visual.backend.gerencial.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.gerencial.dominio.modelo.Alerta;
import com.visual.backend.gerencial.dominio.modelo.FiltroAlertas;

public interface RepositorioAlertas {

    /** Alertas pendientes o atendidas (no resueltas). */
    List<Alerta> abiertas();

    List<Alerta> buscar(FiltroAlertas filtro);

    Optional<Alerta> buscarPorId(Long id);

    default Alerta obtener(Long id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("la alerta", id));
    }

    Alerta guardar(Alerta alerta);
}
