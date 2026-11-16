package com.visual.backend.subcontratacion.aplicacion.servicio;

import java.util.List;

import org.springframework.stereotype.Component;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.subcontratacion.dominio.modelo.ProveedorReferencia;
import com.visual.backend.subcontratacion.dominio.modelo.ProyectoReferencia;
import com.visual.backend.subcontratacion.dominio.puerto.ConsultaReferencias;

/** Resuelve y valida el proyecto, evento, proveedor y servicio a los que se imputa una operacion. */
@Component
public class ReferenciasImputacion {

    private final ConsultaReferencias consulta;

    public ReferenciasImputacion(ConsultaReferencias consulta) {
        this.consulta = consulta;
    }

    public ElementoCatalogo proyectoVigente(Integer idProyecto) {
        return consulta.proyecto(idProyecto).map(ProyectoReferencia::exigirVigente)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proyecto", idProyecto));
    }

    /** Evento opcional: nulo si no se indica. */
    public ElementoCatalogo evento(Integer idEvento, Integer idProyecto) {
        return idEvento == null ? null : consulta.eventoDelProyecto(idEvento, idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el evento del proyecto", idEvento));
    }

    public ElementoCatalogo proveedorVigente(Integer idProveedor) {
        return consulta.proveedor(idProveedor).map(ProveedorReferencia::exigirVigente)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proveedor", idProveedor));
    }

    /** Servicios que ofrece el proveedor, para elegir el subcontratado. */
    public List<ElementoCatalogo> servicios(Integer idProveedor) {
        consulta.proveedor(idProveedor).orElseThrow(() -> new RecursoNoEncontradoExcepcion("el proveedor",
                idProveedor));
        return consulta.serviciosDelProveedor(idProveedor);
    }

    /** Servicio opcional del proveedor: nulo si no se indica. */
    public ElementoCatalogo servicio(Integer idServicio, Integer idProveedor) {
        return idServicio == null ? null : consulta.servicioDelProveedor(idServicio, idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el servicio del proveedor", idServicio));
    }
}
