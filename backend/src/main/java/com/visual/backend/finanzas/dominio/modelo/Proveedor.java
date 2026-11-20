package com.visual.backend.finanzas.dominio.modelo;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.compartido.dominio.ReglasDocumento;
import com.visual.backend.compartido.dominio.ReglasTexto;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Proveedor de compras, alquileres y servicios externos: persona natural,
 * empresa o institucion, con documentos opcionales que no se repiten. Se
 * anula (no se elige en operaciones nuevas) o se elimina logicamente.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Proveedor {

    /** Tipo de proveedor cuyo nombre solo admite letras. */
    public static final String PERSONA_NATURAL = "Persona natural";

    private Integer id;
    private ElementoCatalogo tipo;
    private String razonSocial;
    private String direccion;
    private List<DocumentoProveedor> documentos;
    private String telefono;
    private String email;
    private List<String> servicios;
    private boolean activo;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    public static Proveedor nuevo(DatosProveedor datos, Integer actor, LocalDateTime ahora) {
        Proveedor proveedor = Proveedor.builder().activo(true).creadoPor(actor).fechaCreacion(ahora).build();
        proveedor.aplicar(datos);
        return proveedor;
    }

    public void actualizar(DatosProveedor datos, Integer actor, LocalDateTime ahora) {
        aplicar(datos);
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    /** Un proveedor anulado conserva su historial, pero no se elige en operaciones nuevas. */
    public void cambiarActivo(boolean nuevoEstado, Integer actor, LocalDateTime ahora) {
        if (activo == nuevoEstado) {
            throw new ReglaNegocioExcepcion("El proveedor ya está " + (activo ? "activo" : "anulado"));
        }
        activo = nuevoEstado;
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public void exigirActivo() {
        if (!activo) {
            throw new ReglaNegocioExcepcion("El proveedor " + razonSocial + " está anulado; actívelo para usarlo");
        }
    }

    /** Documento principal para listados: el NIT si lo tiene; si no, el primero. */
    public String documentoPrincipal() {
        return documentos.stream().filter(documento -> ReglasDocumento.NIT.equals(documento.tipo().nombre()))
                .findFirst().or(() -> documentos.stream().findFirst())
                .map(documento -> documento.tipo().nombre() + " " + documento.descripcion()).orElse(null);
    }

    /** Libera sus documentos para que otro registro los pueda usar. */
    public void eliminar(Integer actor, LocalDateTime ahora) {
        documentos = List.of();
        modificadoPor = actor;
        fechaModificacion = ahora;
        fechaEliminacion = ahora;
    }

    public Referencia comoReferencia() {
        return new Referencia(id, razonSocial);
    }

    private void aplicar(DatosProveedor datos) {
        if (datos.razonSocial() == null || datos.razonSocial().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el nombre o la razón social del proveedor");
        }
        if (PERSONA_NATURAL.equals(datos.tipo().nombre())) {
            ReglasTexto.exigirNombrePersona(datos.razonSocial(), "nombre");
        } else {
            ReglasTexto.exigirRazonSocial(datos.razonSocial(), "razón social");
        }
        List<DocumentoProveedor> nuevos = datos.documentos() == null ? List.of() : List.copyOf(datos.documentos());
        Set<String> claves = new LinkedHashSet<>();
        for (DocumentoProveedor documento : nuevos) {
            ReglasDocumento.validar(documento.tipo(), documento.numero(), documento.complemento());
            if (!claves.add(documento.clave())) {
                throw new ReglaNegocioExcepcion("El documento " + documento.descripcion() + " está repetido");
            }
        }
        tipo = datos.tipo();
        razonSocial = datos.razonSocial().trim();
        direccion = textoOpcional(datos.direccion());
        documentos = nuevos;
        telefono = textoOpcional(datos.telefono());
        email = textoOpcional(datos.email());
        servicios = sinRepetir(datos.servicios());
    }

    /** Descarta servicios vacios y repetidos (sin distinguir mayusculas). */
    private static List<String> sinRepetir(List<String> servicios) {
        Set<String> claves = new LinkedHashSet<>();
        return servicios == null ? List.of() : servicios.stream()
                .filter(servicio -> servicio != null && !servicio.isBlank())
                .map(String::trim)
                .filter(servicio -> claves.add(servicio.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
