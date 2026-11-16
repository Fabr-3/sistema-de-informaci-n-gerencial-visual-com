package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Locale;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Modulo LED de una pantalla, rastreado individualmente por su codigo. */
public class ModuloLed {

    private final Integer id;
    private Integer idActivoPadre;
    private final String codigo;
    private EstadoModuloLed estado;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    public ModuloLed(Integer id, Integer idActivoPadre, String codigo, EstadoModuloLed estado,
                     LocalDateTime fechaCreacion, LocalDateTime fechaModificacion) {
        this.id = id;
        this.idActivoPadre = idActivoPadre;
        this.codigo = codigo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
    }

    public static ModuloLed nuevo(Activo pantalla, String codigo, LocalDateTime ahora) {
        validarPantalla(pantalla);
        return new ModuloLed(null, pantalla.getId(), normalizarCodigo(codigo), EstadoModuloLed.ACTIVO, ahora, null);
    }

    public static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Retira este modulo (activo o defectuoso) y devuelve el que ocupa su
     * lugar en la misma pantalla.
     */
    public ModuloLed reemplazar(String codigoNuevo, LocalDateTime ahora) {
        if (estado == EstadoModuloLed.RETIRADO) {
            throw new ReglaNegocioExcepcion("El módulo " + codigo + " ya está retirado");
        }
        String nuevo = normalizarCodigo(codigoNuevo);
        if (nuevo.equals(codigo)) {
            throw new ReglaNegocioExcepcion("El módulo nuevo debe tener otro código");
        }
        estado = EstadoModuloLed.RETIRADO;
        fechaModificacion = ahora;
        return new ModuloLed(null, idActivoPadre, nuevo, EstadoModuloLed.ACTIVO, ahora, null);
    }

    /** Solo un modulo activo pasa a otra pantalla vigente. */
    public void reasignar(Activo destino, LocalDateTime ahora) {
        if (estado != EstadoModuloLed.ACTIVO) {
            throw new ReglaNegocioExcepcion("Solo se reasignan módulos activos");
        }
        if (destino.getId().equals(idActivoPadre)) {
            throw new ReglaNegocioExcepcion("El módulo ya pertenece a " + destino.getCodigoInventario());
        }
        validarPantalla(destino);
        idActivoPadre = destino.getId();
        fechaModificacion = ahora;
    }

    private static void validarPantalla(Activo pantalla) {
        if (pantalla.estaFueraDeInventario()) {
            throw new ReglaNegocioExcepcion("No se pueden agregar módulos a un activo dado de baja o vendido");
        }
    }

    /** Un modulo retirado no vuelve a usarse: se registra uno nuevo en su lugar. */
    public void cambiarEstado(EstadoModuloLed nuevoEstado, LocalDateTime ahora) {
        if (estado == EstadoModuloLed.RETIRADO) {
            throw new ReglaNegocioExcepcion("El módulo " + codigo + " está retirado y no puede cambiar de estado");
        }
        estado = nuevoEstado;
        fechaModificacion = ahora;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdActivoPadre() {
        return idActivoPadre;
    }

    public String getCodigo() {
        return codigo;
    }

    public EstadoModuloLed getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}
