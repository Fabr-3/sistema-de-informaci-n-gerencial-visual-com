package com.visual.backend.logistica.aplicacion.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.dominio.puerto.ContextoSolicitud;
import com.visual.backend.logistica.aplicacion.dto.VistaAsignacionPropia;
import com.visual.backend.logistica.aplicacion.dto.VistaMisAsignaciones;
import com.visual.backend.logistica.dominio.modelo.Periodo;
import com.visual.backend.logistica.dominio.puerto.RepositorioAsignaciones;
import com.visual.backend.logistica.dominio.puerto.RepositorioPersonal;

/** Asignaciones del tecnico o sub-contratado que inicio sesion, desde hoy en adelante. */
@Service
public class ServicioAsignacionesPropias {

    private final RepositorioPersonal personal;
    private final RepositorioAsignaciones asignaciones;
    private final ContextoSolicitud contexto;
    private final Clock reloj;

    public ServicioAsignacionesPropias(RepositorioPersonal personal, RepositorioAsignaciones asignaciones,
                                       ContextoSolicitud contexto, Clock reloj) {
        this.personal = personal;
        this.asignaciones = asignaciones;
        this.contexto = contexto;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public VistaMisAsignaciones mias() {
        Integer idUsuario = contexto.idUsuarioActual();
        return personal.buscarPorUsuario(idUsuario)
                .map(persona -> new VistaMisAsignaciones(true, persona.nombreCompleto(),
                        asignaciones.listarDePersonal(persona.getId(), LocalDate.now(reloj).atStartOfDay()).stream()
                                .map(propia -> new VistaAsignacionPropia(propia.id(), propia.evento(), propia.rol(),
                                        propia.inicio(), propia.fin(), propia.detallesTransporte(),
                                        asignaciones.cruces(propia.idPersonal(),
                                                new Periodo(propia.inicio(), propia.fin()), propia.idPlan())))
                                .toList()))
                .orElseGet(() -> new VistaMisAsignaciones(false, null, List.of()));
    }
}
