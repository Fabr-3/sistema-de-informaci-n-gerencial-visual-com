package com.visual.backend.logistica.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;
import com.visual.backend.logistica.aplicacion.dto.ComandoPersonal;
import com.visual.backend.logistica.aplicacion.dto.PersonalDisponible;
import com.visual.backend.logistica.aplicacion.dto.VistaPersonal;
import com.visual.backend.logistica.dominio.modelo.Cruce;
import com.visual.backend.logistica.dominio.modelo.DatosPersonal;
import com.visual.backend.logistica.dominio.modelo.DocumentoPersonal;
import com.visual.backend.logistica.dominio.modelo.Filtros;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;
import com.visual.backend.logistica.dominio.puerto.RepositorioAsignaciones;
import com.visual.backend.logistica.dominio.puerto.RepositorioCatalogosLogistica;
import com.visual.backend.logistica.dominio.puerto.RepositorioPersonal;

/** Registro del personal tecnico y consulta de su disponibilidad. */
@Service
public class ServicioPersonal {

    public static final String ENTIDAD = "personal_tecnico";

    private final RepositorioPersonal repositorio;
    private final RepositorioCatalogosLogistica catalogos;
    private final RepositorioAsignaciones asignaciones;
    private final ServicioAuditoria auditoria;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioPersonal(RepositorioPersonal repositorio, RepositorioCatalogosLogistica catalogos,
                            RepositorioAsignaciones asignaciones, ServicioAuditoria auditoria,
                            ContextoSolicitud contexto, Clock reloj) {
        this.repositorio = repositorio;
        this.catalogos = catalogos;
        this.asignaciones = asignaciones;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<VistaPersonal> listar(Filtros.Personal filtro, SolicitudPagina solicitud) {
        return repositorio.buscar(filtro, solicitud).mapear(VistaPersonal::de);
    }

    @Transactional(readOnly = true)
    public VistaPersonal obtener(Integer id) {
        return VistaPersonal.de(repositorio.obtener(id));
    }

    @Transactional
    public VistaPersonal crear(ComandoPersonal comando) {
        PersonalTecnico personal = PersonalTecnico.nuevo(datos(comando, null), contexto.idUsuarioActual(),
                LocalDateTime.now(reloj));
        VistaPersonal creado = VistaPersonal.de(repositorio.guardar(personal));
        auditoria.registrarOperacion(AccionAuditoria.CREAR, ENTIDAD, creado.id(), null, creado.comoMapaAuditoria(),
                null);
        return creado;
    }

    @Transactional
    public VistaPersonal actualizar(Integer id, ComandoPersonal comando) {
        PersonalTecnico personal = repositorio.obtener(id);
        VistaPersonal anterior = VistaPersonal.de(personal);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (personal.isActivo() && !comando.activo()
                && !asignaciones.listarDePersonal(id, ahora).isEmpty()) {
            throw new ReglaNegocioExcepcion(personal.nombreCompleto()
                    + " tiene asignaciones pendientes; reasígnelas antes de desactivarlo");
        }
        personal.actualizar(datos(comando, id), contexto.idUsuarioActual(), ahora);
        VistaPersonal actualizado = VistaPersonal.de(repositorio.guardar(personal));
        auditoria.registrarOperacion(AccionAuditoria.ACTUALIZAR, ENTIDAD, id, anterior.comoMapaAuditoria(),
                actualizado.comoMapaAuditoria(), null);
        return actualizado;
    }

    /** Personal activo con sus asignaciones en otros planes que se cruzan con el periodo. */
    @Transactional(readOnly = true)
    public List<PersonalDisponible> disponibles(Periodo periodo, Integer idPlanExcluido) {
        Map<Integer, List<Cruce>> cruces = asignaciones.cruces(null, periodo, idPlanExcluido).stream()
                .collect(Collectors.groupingBy(Cruce::idPersonal));
        return repositorio.listarActivos().stream()
                .map(personal -> new PersonalDisponible(personal.getId(), personal.nombreCompleto(),
                        personal.getEspecialidades().stream().map(ElementoCatalogo::nombre).toList(),
                        cruces.getOrDefault(personal.getId(), List.of())))
                .toList();
    }

    private DatosPersonal datos(ComandoPersonal comando, Integer idPersonal) {
        return new DatosPersonal(comando.nombres(), comando.apellidos(), comando.telefono(),
                documento(comando, idPersonal), especialidades(comando), usuario(comando, idPersonal),
                comando.activo());
    }

    /** Un documento de identidad no puede pertenecer a dos personas. */
    private DocumentoPersonal documento(ComandoPersonal comando, Integer idPersonal) {
        ElementoCatalogo tipo = catalogos.buscarTipoDocumento(comando.idTipoDocumento())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el tipo de documento",
                        comando.idTipoDocumento()));
        String numero = comando.numeroDocumento().trim();
        if (repositorio.existeDocumento(tipo.id(), numero, idPersonal)) {
            throw new DatoDuplicadoExcepcion("El " + tipo.nombre() + " " + numero
                    + " ya está registrado en otra persona");
        }
        return new DocumentoPersonal(tipo, numero);
    }

    private List<ElementoCatalogo> especialidades(ComandoPersonal comando) {
        if (comando.idsEspecialidades() == null) {
            return List.of();
        }
        return comando.idsEspecialidades().stream()
                .map(id -> catalogos.buscarEspecialidad(id)
                        .orElseThrow(() -> new RecursoNoEncontradoExcepcion("la especialidad", id)))
                .toList();
    }

    /** Solo se vinculan cuentas de tecnicos o sub-contratados, y cada una a una sola persona. */
    private UsuarioVinculado usuario(ComandoPersonal comando, Integer idPersonal) {
        if (comando.idUsuario() == null) {
            return null;
        }
        UsuarioVinculado usuario = catalogos.buscarUsuarioVinculable(comando.idUsuario())
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "Solo se pueden vincular cuentas de técnicos o sub-contratados"));
        if (repositorio.usuarioVinculadoAOtro(usuario.id(), idPersonal)) {
            throw new DatoDuplicadoExcepcion("La cuenta " + usuario.nombreUsuario()
                    + " ya está vinculada a otra persona");
        }
        return usuario;
    }
}
