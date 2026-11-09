package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.ContactoCliente;
import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.modelo.Cotizacion;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Evento;
import com.visual.backend.comercial.dominio.modelo.EventoCalendario;
import com.visual.backend.comercial.dominio.modelo.LineaCotizacion;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Proyecto;
import com.visual.backend.comercial.dominio.modelo.ResumenCliente;
import com.visual.backend.comercial.dominio.modelo.Servicio;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ContactoClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ContratoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.CotizacionEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.DocumentoClienteEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.EventoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.LineaCotizacionEmbebida;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.LugarEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ProyectoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ServicioEntidad;

/** Conversion de entidades JPA a modelos de dominio del modulo comercial. */
final class MapeadorComercial {

    private MapeadorComercial() {
    }

    static ElementoCatalogo catalogo(Integer id, String nombre) {
        return new ElementoCatalogo(id, nombre);
    }

    static Servicio aDominio(ServicioEntidad entidad) {
        return new Servicio(entidad.getId(), entidad.getNombre(), entidad.getDescripcion(), entidad.getUnidadMedida(),
                entidad.getPrecioReferencial(), entidad.isActivo());
    }

    static Lugar aDominio(LugarEntidad entidad) {
        return new Lugar(entidad.getId(), entidad.getNombre(), entidad.getDireccion(), entidad.getCiudad());
    }

    static ResumenCliente resumen(ClienteEntidad entidad) {
        return new ResumenCliente(entidad.getId(), entidad.getRazonSocial());
    }

    static Cliente aDominio(ClienteEntidad entidad) {
        return new Cliente(entidad.getId(), catalogo(entidad.getTipo().getId(), entidad.getTipo().getNombre()),
                entidad.getRazonSocial(), entidad.getDireccion(),
                entidad.getDocumentos().stream().map(MapeadorComercial::aDominio).toList(),
                entidad.getContactos().stream().map(MapeadorComercial::aDominio).toList(),
                entidad.getCreadoPor(), entidad.getFechaCreacion(), entidad.getModificadoPor(),
                entidad.getFechaModificacion(), entidad.getFechaEliminacion());
    }

    static DocumentoCliente aDominio(DocumentoClienteEntidad entidad) {
        return new DocumentoCliente(entidad.getId(), catalogo(entidad.getTipo().getId(), entidad.getTipo().getNombre()),
                entidad.getNumero());
    }

    static ContactoCliente aDominio(ContactoClienteEntidad entidad) {
        return new ContactoCliente(entidad.getId(), catalogo(entidad.getTipo().getId(), entidad.getTipo().getNombre()),
                entidad.getValor(), entidad.getReferencia());
    }

    static Cotizacion aDominio(CotizacionEntidad entidad) {
        return Cotizacion.builder()
                .id(entidad.getId())
                .numero(entidad.getNumero())
                .cliente(resumen(entidad.getCliente()))
                .titulo(entidad.getTitulo())
                .fechaEmision(entidad.getFechaEmision())
                .fechaValidez(entidad.getFechaValidez())
                .fechaEventoInicio(entidad.getFechaEventoInicio())
                .fechaEventoFin(entidad.getFechaEventoFin())
                .estado(entidad.getEstado())
                .condiciones(entidad.getCondiciones())
                .lineas(entidad.getLineas().stream().map(MapeadorComercial::aDominio).toList())
                .resueltoPor(entidad.getResueltoPor())
                .nombreResolutor(nombreMostrado(entidad.getResolutor()))
                .fechaResolucion(entidad.getFechaResolucion())
                .motivoRechazo(entidad.getMotivoRechazo())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .fechaEliminacion(entidad.getFechaEliminacion())
                .build();
    }

    static LineaCotizacion aDominio(LineaCotizacionEmbebida linea) {
        ServicioEntidad servicio = linea.getServicio();
        return new LineaCotizacion(servicio.getId(), servicio.getNombre(), servicio.getUnidadMedida(),
                linea.getCantidad(), linea.getPrecioUnitario(), linea.getDescuento());
    }

    static Proyecto aDominio(ProyectoEntidad entidad) {
        CotizacionEntidad cotizacion = entidad.getCotizacion();
        return Proyecto.builder()
                .id(entidad.getId())
                .idCotizacion(cotizacion.getId())
                .numeroCotizacion(cotizacion.getNumero())
                .cliente(resumen(cotizacion.getCliente()))
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .fechaInicio(entidad.getFechaInicio())
                .fechaFin(entidad.getFechaFin())
                .estado(entidad.getEstado())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static Evento aDominio(EventoEntidad entidad) {
        return Evento.builder()
                .id(entidad.getId())
                .idProyecto(entidad.getProyecto().getId())
                .lugar(aDominio(entidad.getLugar()))
                .nombre(entidad.getNombre())
                .fechaInicio(entidad.getFechaInicio())
                .fechaFin(entidad.getFechaFin())
                .estado(entidad.getEstado())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static EventoCalendario calendario(EventoEntidad entidad) {
        ProyectoEntidad proyecto = entidad.getProyecto();
        return new EventoCalendario(entidad.getId(), entidad.getNombre(), entidad.getFechaInicio(),
                entidad.getFechaFin(), entidad.getEstado(), proyecto.getId(), proyecto.getNombre(),
                proyecto.getCotizacion().getCliente().getRazonSocial(), entidad.getLugar().getNombre(),
                entidad.getLugar().getCiudad());
    }

    static Contrato aDominio(ContratoEntidad entidad) {
        return Contrato.builder()
                .id(entidad.getId())
                .idProyecto(entidad.getIdProyecto())
                .numero(entidad.getNumero())
                .fechaFirma(entidad.getFechaFirma())
                .terminos(entidad.getTerminos())
                .archivoFirmado(entidad.getArchivoAdjunto())
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    private static String nombreMostrado(UsuarioEntidad usuario) {
        return usuario == null ? null : usuario.nombreCompleto();
    }
}
