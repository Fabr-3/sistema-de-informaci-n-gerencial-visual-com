package com.visual.backend.finanzas.dominio.modelo;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Proveedor de compras, alquileres y servicios externos. El NIT es opcional
 * (proveedores informales), pero no se repite. Se elimina logicamente.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Proveedor {

    private Integer id;
    private String razonSocial;
    private String nit;
    private String telefono;
    private String email;
    private List<String> servicios;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    public static Proveedor nuevo(DatosProveedor datos, Integer actor, LocalDateTime ahora) {
        Proveedor proveedor = Proveedor.builder().creadoPor(actor).fechaCreacion(ahora).build();
        proveedor.aplicar(datos);
        return proveedor;
    }

    public void actualizar(DatosProveedor datos, Integer actor, LocalDateTime ahora) {
        aplicar(datos);
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    /** Libera el NIT para que otro registro lo pueda usar. */
    public void eliminar(Integer actor, LocalDateTime ahora) {
        nit = null;
        modificadoPor = actor;
        fechaModificacion = ahora;
        fechaEliminacion = ahora;
    }

    public Referencia comoReferencia() {
        return new Referencia(id, razonSocial);
    }

    private void aplicar(DatosProveedor datos) {
        if (datos.razonSocial() == null || datos.razonSocial().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique la razón social del proveedor");
        }
        razonSocial = datos.razonSocial().trim();
        nit = textoOpcional(datos.nit());
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
