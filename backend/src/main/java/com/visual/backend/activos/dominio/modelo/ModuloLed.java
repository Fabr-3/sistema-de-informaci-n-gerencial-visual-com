package com.visual.backend.activos.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Locale;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Modulo LED de una pantalla, rastreado individualmente por su codigo. */
public class ModuloLed {

    private final Integer id;
    private final Integer idActivoPadre;
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
        if (pantalla.estaDadoDeBaja()) {
            throw new ReglaNegocioExcepcion("No se pueden agregar módulos a un activo dado de baja");
        }
        return new ModuloLed(null, pantalla.getId(), codigo.trim().toUpperCase(Locale.ROOT),
                EstadoModuloLed.ACTIVO, ahora, null);
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
