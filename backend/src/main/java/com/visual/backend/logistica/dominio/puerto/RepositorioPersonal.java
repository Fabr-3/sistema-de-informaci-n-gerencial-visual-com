package com.visual.backend.logistica.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;

public interface RepositorioPersonal {

    Optional<PersonalTecnico> buscarPorId(Integer id);

    default PersonalTecnico obtener(Integer id) {
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el personal técnico", id));
    }

    Pagina<PersonalTecnico> buscar(Filtros.Personal filtro, SolicitudPagina solicitud);

    /** Todo el personal activo, ordenado por nombre (para asignar). */
    List<PersonalTecnico> listarActivos();

    Optional<PersonalTecnico> buscarPorUsuario(Integer idUsuario);

    boolean existeDocumento(Integer idTipoDocumento, String numero, Integer idExcluido);

    boolean usuarioVinculadoAOtro(Integer idUsuario, Integer idExcluido);

    PersonalTecnico guardar(PersonalTecnico personal);
}
