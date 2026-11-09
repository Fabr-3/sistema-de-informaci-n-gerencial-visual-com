package com.visual.backend.comercial.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Contrato de un proyecto. Su documento PDF se genera con la plantilla
 * estandar a partir de los datos del proyecto, la cotizacion y los terminos.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Contrato {

    private static final String EXTENSION_PDF = ".pdf";

    private Integer id;
    private Integer idProyecto;
    private String numero;
    private LocalDate fechaFirma;
    private String terminos;
    /** Nombre del archivo PDF del contrato firmado; null si aun no se adjunto. */
    private String archivoFirmado;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static Contrato nuevo(Proyecto proyecto, String numero, LocalDate fechaFirma, String terminos,
                                 Integer actor, LocalDateTime ahora) {
        proyecto.exigirVigente("registrar contratos");
        validarFirma(fechaFirma, ahora);
        return Contrato.builder()
                .idProyecto(proyecto.getId())
                .numero(numero)
                .fechaFirma(fechaFirma)
                .terminos(textoOpcional(terminos))
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    public void actualizar(LocalDate nuevaFechaFirma, String nuevosTerminos, Integer actor, LocalDateTime ahora) {
        validarFirma(nuevaFechaFirma, ahora);
        fechaFirma = nuevaFechaFirma;
        terminos = textoOpcional(nuevosTerminos);
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    /** El escaneo firmado se guarda con el numero del contrato; reemplaza al anterior. */
    public void adjuntarFirmado(Integer actor, LocalDateTime ahora) {
        archivoFirmado = numero + EXTENSION_PDF;
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public boolean tieneArchivoFirmado() {
        return archivoFirmado != null;
    }

    private static void validarFirma(LocalDate fechaFirma, LocalDateTime ahora) {
        if (fechaFirma == null || fechaFirma.isAfter(ahora.toLocalDate())) {
            throw new ReglaNegocioExcepcion("La fecha de firma no puede ser futura");
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
