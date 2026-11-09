package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Cliente de la empresa (persona, empresa o institucion). Debe tener al menos
 * un documento (regla tambien protegida por un trigger diferido en la base).
 */
public class Cliente {

    private final Integer id;
    private ElementoCatalogo tipo;
    private String razonSocial;
    private String direccion;
    private List<DocumentoCliente> documentos;
    private List<ContactoCliente> contactos;
    private final Integer creadoPor;
    private final LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    public Cliente(Integer id, ElementoCatalogo tipo, String razonSocial, String direccion,
                   List<DocumentoCliente> documentos, List<ContactoCliente> contactos, Integer creadoPor,
                   LocalDateTime fechaCreacion, Integer modificadoPor, LocalDateTime fechaModificacion,
                   LocalDateTime fechaEliminacion) {
        this.id = id;
        this.tipo = tipo;
        this.razonSocial = razonSocial;
        this.direccion = direccion;
        this.documentos = List.copyOf(documentos);
        this.contactos = List.copyOf(contactos);
        this.creadoPor = creadoPor;
        this.fechaCreacion = fechaCreacion;
        this.modificadoPor = modificadoPor;
        this.fechaModificacion = fechaModificacion;
        this.fechaEliminacion = fechaEliminacion;
    }

    public static Cliente nuevo(ElementoCatalogo tipo, String razonSocial, String direccion,
                                List<DocumentoCliente> documentos, List<ContactoCliente> contactos, Integer actor,
                                LocalDateTime ahora) {
        validar(razonSocial, documentos, contactos);
        return new Cliente(null, tipo, razonSocial.trim(), textoOpcional(direccion), documentos, contactos, actor,
                ahora, null, null, null);
    }

    public void actualizar(ElementoCatalogo nuevoTipo, String nuevaRazonSocial, String nuevaDireccion,
                           List<DocumentoCliente> nuevosDocumentos, List<ContactoCliente> nuevosContactos,
                           Integer actor, LocalDateTime ahora) {
        validar(nuevaRazonSocial, nuevosDocumentos, nuevosContactos);
        tipo = nuevoTipo;
        razonSocial = nuevaRazonSocial.trim();
        direccion = textoOpcional(nuevaDireccion);
        documentos = List.copyOf(nuevosDocumentos);
        contactos = List.copyOf(nuevosContactos);
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public void eliminar(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
        fechaEliminacion = ahora;
    }

    public ResumenCliente resumen() {
        return new ResumenCliente(id, razonSocial);
    }

    private static void validar(String razonSocial, List<DocumentoCliente> documentos,
                                List<ContactoCliente> contactos) {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el nombre o la razón social del cliente");
        }
        if (documentos == null || documentos.isEmpty()) {
            throw new ReglaNegocioExcepcion("El cliente debe tener al menos un documento (CI o NIT)");
        }
        Set<String> claves = new HashSet<>();
        for (DocumentoCliente documento : documentos) {
            if (!claves.add(documento.tipo().id() + ":" + documento.numero().toUpperCase(Locale.ROOT))) {
                throw new ReglaNegocioExcepcion("El documento " + documento.numero() + " está repetido");
            }
        }
        Set<String> valores = new HashSet<>();
        for (ContactoCliente contacto : contactos) {
            if (!valores.add(contacto.tipo().id() + ":" + contacto.valor().toLowerCase(Locale.ROOT))) {
                throw new ReglaNegocioExcepcion("El contacto " + contacto.valor() + " está repetido");
            }
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public Integer getId() {
        return id;
    }

    public ElementoCatalogo getTipo() {
        return tipo;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getDireccion() {
        return direccion;
    }

    public List<DocumentoCliente> getDocumentos() {
        return documentos;
    }

    public List<ContactoCliente> getContactos() {
        return contactos;
    }

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Integer getModificadoPor() {
        return modificadoPor;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public LocalDateTime getFechaEliminacion() {
        return fechaEliminacion;
    }
}
