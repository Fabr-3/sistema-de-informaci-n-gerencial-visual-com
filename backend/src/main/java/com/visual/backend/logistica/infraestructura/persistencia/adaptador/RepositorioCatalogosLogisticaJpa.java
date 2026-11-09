package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoDocumentoJpaRepositorio;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;
import com.visual.backend.logistica.dominio.puerto.RepositorioCatalogosLogistica;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.EspecialidadJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.RolEventoJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.UsuarioVinculableJpaRepositorio;

@Repository
public class RepositorioCatalogosLogisticaJpa implements RepositorioCatalogosLogistica {

    /** Roles cuyas cuentas pueden consultar sus propias asignaciones. */
    private static final List<CodigoRol> ROLES_VINCULABLES = List.of(CodigoRol.TECNICO,
            CodigoRol.SUBCONTRATADO_TEMPORAL);
    private static final Sort POR_NOMBRE = Sort.by("nombre");
    private static final Sort POR_ID = Sort.by("id");

    private final RolEventoJpaRepositorio roles;
    private final EspecialidadJpaRepositorio especialidades;
    private final TipoDocumentoJpaRepositorio tiposDocumento;
    private final UsuarioVinculableJpaRepositorio usuarios;

    public RepositorioCatalogosLogisticaJpa(RolEventoJpaRepositorio roles, EspecialidadJpaRepositorio especialidades,
                                            TipoDocumentoJpaRepositorio tiposDocumento,
                                            UsuarioVinculableJpaRepositorio usuarios) {
        this.roles = roles;
        this.especialidades = especialidades;
        this.tiposDocumento = tiposDocumento;
        this.usuarios = usuarios;
    }

    @Override
    public List<ElementoCatalogo> rolesEvento() {
        return roles.findAll(POR_NOMBRE).stream().map(rol -> new ElementoCatalogo(rol.getId(), rol.getNombre()))
                .toList();
    }

    @Override
    public List<ElementoCatalogo> especialidades() {
        return especialidades.findAll(POR_NOMBRE).stream()
                .map(especialidad -> new ElementoCatalogo(especialidad.getId(), especialidad.getNombre())).toList();
    }

    @Override
    public List<ElementoCatalogo> tiposDocumento() {
        return tiposDocumento.findAll(POR_ID).stream().map(tipo -> new ElementoCatalogo(tipo.getId(), tipo.getNombre()))
                .toList();
    }

    @Override
    public List<UsuarioVinculado> usuariosVinculables() {
        return usuarios.listar(ROLES_VINCULABLES).stream().map(MapeadorLogistica::usuario).toList();
    }

    @Override
    public Optional<ElementoCatalogo> buscarRolEvento(Integer id) {
        return roles.findById(id).map(rol -> new ElementoCatalogo(rol.getId(), rol.getNombre()));
    }

    @Override
    public Optional<ElementoCatalogo> buscarEspecialidad(Integer id) {
        return especialidades.findById(id)
                .map(especialidad -> new ElementoCatalogo(especialidad.getId(), especialidad.getNombre()));
    }

    @Override
    public Optional<ElementoCatalogo> buscarTipoDocumento(Integer id) {
        return tiposDocumento.findById(id).map(tipo -> new ElementoCatalogo(tipo.getId(), tipo.getNombre()));
    }

    @Override
    public Optional<UsuarioVinculado> buscarUsuarioVinculable(Integer id) {
        return usuarios.buscar(id, ROLES_VINCULABLES).map(MapeadorLogistica::usuario);
    }
}
