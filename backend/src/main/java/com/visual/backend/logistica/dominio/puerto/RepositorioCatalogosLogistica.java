package com.visual.backend.logistica.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;

/** Catalogos de logistica y cuentas de usuario que se pueden vincular al personal. */
public interface RepositorioCatalogosLogistica {

    List<ElementoCatalogo> rolesEvento();

    List<ElementoCatalogo> especialidades();

    List<ElementoCatalogo> tiposDocumento();

    /** Cuentas habilitadas con rol Tecnico o Sub-contratado temporal aun sin vincular. */
    List<UsuarioVinculado> usuariosVinculables();

    Optional<ElementoCatalogo> buscarRolEvento(Integer id);

    Optional<ElementoCatalogo> buscarEspecialidad(Integer id);

    Optional<ElementoCatalogo> buscarTipoDocumento(Integer id);

    Optional<UsuarioVinculado> buscarUsuarioVinculable(Integer id);
}
