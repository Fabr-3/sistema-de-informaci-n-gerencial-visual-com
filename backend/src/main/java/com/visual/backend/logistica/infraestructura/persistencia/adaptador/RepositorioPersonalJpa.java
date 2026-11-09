package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.visual.backend.accesos.infraestructura.persistencia.jpa.UsuarioJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoDocumentoJpaRepositorio;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.infraestructura.persistencia.ConsultasJpa;
import com.visual.backend.logistica.dominio.modelo.DocumentoPersonal;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.puerto.RepositorioPersonal;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.DocumentoPersonalEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.PersonalTecnicoEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.EspecialidadJpaRepositorio;
import com.visual.backend.logistica.infraestructura.persistencia.jpa.PersonalTecnicoJpaRepositorio;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

@Repository
public class RepositorioPersonalJpa implements RepositorioPersonal {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("nombres", "apellidos", "id");
    private static final String SEPARADOR_NOMBRE = " ";

    private final PersonalTecnicoJpaRepositorio personal;
    private final EspecialidadJpaRepositorio especialidades;
    private final TipoDocumentoJpaRepositorio tiposDocumento;
    private final UsuarioJpaRepositorio usuarios;

    public RepositorioPersonalJpa(PersonalTecnicoJpaRepositorio personal, EspecialidadJpaRepositorio especialidades,
                                  TipoDocumentoJpaRepositorio tiposDocumento, UsuarioJpaRepositorio usuarios) {
        this.personal = personal;
        this.especialidades = especialidades;
        this.tiposDocumento = tiposDocumento;
        this.usuarios = usuarios;
    }

    @Override
    public Optional<PersonalTecnico> buscarPorId(Integer id) {
        return personal.findByIdAndFechaEliminacionIsNull(id).map(MapeadorLogistica::aDominio);
    }

    @Override
    public Pagina<PersonalTecnico> buscar(Filtros.Personal filtro, SolicitudPagina solicitud) {
        return ConsultasJpa.pagina(personal.findAll(especificacion(filtro),
                ConsultasJpa.paginacion(solicitud, ORDEN_POR_DEFECTO)).map(MapeadorLogistica::aDominio));
    }

    @Override
    public List<PersonalTecnico> listarActivos() {
        return personal.findByActivoTrueAndFechaEliminacionIsNullOrderByNombresAscApellidosAsc().stream()
                .map(MapeadorLogistica::aDominio).toList();
    }

    @Override
    public Optional<PersonalTecnico> buscarPorUsuario(Integer idUsuario) {
        return idUsuario == null ? Optional.empty()
                : personal.findByUsuarioIdAndFechaEliminacionIsNull(idUsuario).map(MapeadorLogistica::aDominio);
    }

    @Override
    public boolean existeDocumento(Integer idTipoDocumento, String numero, Integer idExcluido) {
        return personal.existeDocumento(idTipoDocumento, numero, idExcluido);
    }

    @Override
    public boolean usuarioVinculadoAOtro(Integer idUsuario, Integer idExcluido) {
        return personal.usuarioVinculadoAOtro(idUsuario, idExcluido);
    }

    @Override
    public PersonalTecnico guardar(PersonalTecnico dominio) {
        PersonalTecnicoEntidad entidad = dominio.getId() == null ? new PersonalTecnicoEntidad()
                : personal.getReferenceById(dominio.getId());
        entidad.setNombres(dominio.getNombres());
        entidad.setApellidos(dominio.getApellidos());
        entidad.setTelefono(dominio.getTelefono());
        entidad.setActivo(dominio.isActivo());
        entidad.setUsuario(dominio.getUsuario() == null ? null
                : usuarios.getReferenceById(dominio.getUsuario().id()));
        entidad.setCreadoPor(dominio.getCreadoPor());
        entidad.setFechaCreacion(dominio.getFechaCreacion());
        entidad.setModificadoPor(dominio.getModificadoPor());
        entidad.setFechaModificacion(dominio.getFechaModificacion());
        sincronizarDocumento(entidad, dominio);
        entidad.getEspecialidades().clear();
        dominio.getEspecialidades().forEach(especialidad ->
                entidad.getEspecialidades().add(especialidades.getReferenceById(especialidad.id())));
        return MapeadorLogistica.aDominio(personal.saveAndFlush(entidad));
    }

    /** Un solo documento vigente: se corrige en el lugar si es del mismo tipo; si no, se reemplaza. */
    private void sincronizarDocumento(PersonalTecnicoEntidad entidad, PersonalTecnico dominio) {
        DocumentoPersonal documento = dominio.getDocumento();
        DocumentoPersonalEntidad actual = MapeadorLogistica.documentoVigente(entidad);
        if (actual != null && actual.getTipo().getId().equals(documento.tipo().id())) {
            if (!actual.getNumero().equals(documento.numero())) {
                actual.setNumero(documento.numero());
                actual.setModificadoPor(dominio.getModificadoPor());
                actual.setFechaModificacion(dominio.getFechaModificacion());
            }
            return;
        }
        if (actual != null) {
            entidad.getDocumentos().remove(actual);
        }
        DocumentoPersonalEntidad nuevo = new DocumentoPersonalEntidad();
        nuevo.setPersonal(entidad);
        nuevo.setTipo(tiposDocumento.getReferenceById(documento.tipo().id()));
        nuevo.setNumero(documento.numero());
        nuevo.setCreadoPor(dominio.getModificadoPor() == null ? dominio.getCreadoPor() : dominio.getModificadoPor());
        nuevo.setFechaCreacion(dominio.getFechaModificacion() == null ? dominio.getFechaCreacion()
                : dominio.getFechaModificacion());
        entidad.getDocumentos().add(nuevo);
    }

    /** Excluye eliminados; el texto busca en nombres, apellidos y numero de documento. */
    private static Specification<PersonalTecnicoEntidad> especificacion(Filtros.Personal filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isNull(raiz.get("fechaEliminacion")));
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = ConsultasJpa.patronContiene(filtro.texto());
                Subquery<Integer> documentos = consulta.subquery(Integer.class);
                var documento = documentos.from(DocumentoPersonalEntidad.class);
                documentos.select(documento.get("personal").get("id")).where(
                        cb.like(cb.lower(documento.get("numero")), patron, ConsultasJpa.ESCAPE_LIKE));
                condiciones.add(cb.or(
                        cb.like(cb.lower(cb.concat(cb.concat(raiz.get("nombres"), SEPARADOR_NOMBRE),
                                raiz.get("apellidos"))), patron, ConsultasJpa.ESCAPE_LIKE),
                        raiz.get("id").in(documentos)));
            }
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            if (filtro.idEspecialidad() != null) {
                condiciones.add(cb.equal(raiz.join("especialidades").get("id"), filtro.idEspecialidad()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
