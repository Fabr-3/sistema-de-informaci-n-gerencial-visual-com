package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Trabajo de una persona en un evento, con su rol y horario. Debe quedar
 * dentro de la ventana del plan; los cruces con otros eventos solo se alertan.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Asignacion {

    private Integer id;
    private Integer idPlan;
    private ResumenPersonal personal;
    private ElementoCatalogo rol;
    private Periodo periodo;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static Asignacion nueva(PlanLogistico plan, EventoLogistico evento, PersonalTecnico personal,
                                   ElementoCatalogo rol, Periodo periodo, Integer actor, LocalDateTime ahora) {
        validar(plan, evento, personal, periodo);
        return Asignacion.builder()
                .idPlan(plan.getId())
                .personal(personal.resumen())
                .rol(rol)
                .periodo(periodo)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void actualizar(PlanLogistico plan, EventoLogistico evento, PersonalTecnico nuevoPersonal,
                           ElementoCatalogo nuevoRol, Periodo nuevoPeriodo, Integer actor, LocalDateTime ahora) {
        validar(plan, evento, nuevoPersonal, nuevoPeriodo);
        personal = nuevoPersonal.resumen();
        rol = nuevoRol;
        periodo = nuevoPeriodo;
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validar(PlanLogistico plan, EventoLogistico evento, PersonalTecnico personal,
                                Periodo periodo) {
        evento.exigirEditable();
        personal.exigirActivo();
        Periodo ventana = plan.ventana(evento);
        if (!ventana.contiene(periodo)) {
            throw new ReglaNegocioExcepcion("La asignación debe quedar dentro del plan: del "
                    + PlanLogistico.formato(ventana.inicio()) + " al " + PlanLogistico.formato(ventana.fin()));
        }
    }
}
