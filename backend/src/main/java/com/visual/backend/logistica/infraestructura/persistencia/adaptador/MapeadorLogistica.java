package com.visual.backend.logistica.infraestructura.persistencia.adaptador;

import java.util.Comparator;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.LugarEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.logistica.dominio.modelo.Asignacion;
import com.visual.backend.logistica.dominio.modelo.DocumentoPersonal;
import com.visual.backend.logistica.dominio.modelo.EventoLogistico;
import com.visual.backend.logistica.dominio.modelo.Jornada;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.modelo.PlanLogistico;
import com.visual.backend.logistica.dominio.modelo.ResumenPersonal;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.AsignacionEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.DocumentoPersonalEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.JornadaEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.PersonalTecnicoEntidad;
import com.visual.backend.logistica.infraestructura.persistencia.entidad.PlanLogisticoEntidad;

/** Conversion de entidades JPA a modelos de dominio del modulo de logistica. */
final class MapeadorLogistica {

    private MapeadorLogistica() {
    }

    static EventoLogistico evento(EventoEntidad entidad) {
        ProyectoEntidad proyecto = entidad.getProyecto();
        LugarEntidad lugar = entidad.getLugar();
        return new EventoLogistico(entidad.getId(), entidad.getNombre(), entidad.getFechaInicio(),
                entidad.getFechaFin(), entidad.getEstado(), proyecto.getId(), proyecto.getNombre(),
                proyecto.getCotizacion().getCliente().getRazonSocial(), lugar.getNombre(), lugar.getDireccion(),
                lugar.getCiudad());
    }

    static PersonalTecnico aDominio(PersonalTecnicoEntidad entidad) {
        DocumentoPersonalEntidad documento = documentoVigente(entidad);
        return PersonalTecnico.builder()
                .id(entidad.getId())
                .nombres(entidad.getNombres())
                .apellidos(entidad.getApellidos())
                .telefono(entidad.getTelefono())
                .documento(documento == null ? null : new DocumentoPersonal(
                        new ElementoCatalogo(documento.getTipo().getId(), documento.getTipo().getNombre()),
                        documento.getNumero()))
                .especialidades(entidad.getEspecialidades().stream()
                        .map(especialidad -> new ElementoCatalogo(especialidad.getId(), especialidad.getNombre()))
                        .sorted(Comparator.comparing(ElementoCatalogo::nombre))
                        .toList())
                .usuario(usuario(entidad.getUsuario()))
                .activo(entidad.isActivo())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static UsuarioVinculado usuario(UsuarioEntidad usuario) {
        return usuario == null ? null
                : new UsuarioVinculado(usuario.getId(), usuario.getNombreUsuario(), usuario.nombreCompleto());
    }

    static PlanLogistico aDominio(PlanLogisticoEntidad entidad) {
        return PlanLogistico.builder()
                .id(entidad.getId())
                .idEvento(entidad.getIdEvento())
                .detallesTransporte(entidad.getDetallesTransporte())
                .jornadas(entidad.getJornadas().stream().map(MapeadorLogistica::jornada)
                        .sorted(Comparator.comparing(jornada -> jornada.periodo().inicio()))
                        .toList())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static Asignacion aDominio(AsignacionEntidad entidad) {
        PersonalTecnicoEntidad personal = entidad.getPersonal();
        return Asignacion.builder()
                .id(entidad.getId())
                .idPlan(entidad.getIdPlan())
                .personal(new ResumenPersonal(personal.getId(), personal.getNombres() + " " + personal.getApellidos()))
                .rol(new ElementoCatalogo(entidad.getRol().getId(), entidad.getRol().getNombre()))
                .periodo(new Periodo(entidad.getFechaInicio(), entidad.getFechaFin()))
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static DocumentoPersonalEntidad documentoVigente(PersonalTecnicoEntidad entidad) {
        return entidad.getDocumentos().stream().filter(documento -> documento.getFechaEliminacion() == null)
                .findFirst().orElse(null);
    }

    private static Jornada jornada(JornadaEntidad entidad) {
        return new Jornada(entidad.getId(), entidad.getTipo(),
                new Periodo(entidad.getFechaInicio(), entidad.getFechaFin()), entidad.getDescripcion());
    }
}
