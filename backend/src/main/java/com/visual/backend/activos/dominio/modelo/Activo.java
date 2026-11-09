package com.visual.backend.activos.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Equipo controlado por unidad. Concentra las transiciones de estado propias
 * de este modulo: mantenimiento y baja. El builder se reserva para
 * reconstruir activos desde la persistencia (los nuevos usan {@link #nuevo}).
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Activo {

    private Integer id;
    private String codigoInventario;
    private ModeloActivo modelo;
    private String numeroSerie;
    private Bodega bodega;
    private EstadoOperativo estado;
    private LocalDate fechaAdquisicion;
    private BigDecimal costoAdquisicion;
    private String observaciones;
    private String motivoBaja;
    private Integer creadoPor;
    private Integer modificadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    public static Activo nuevo(DatosActivo datos, ModeloActivo modelo, Bodega bodega, Integer actor,
                               LocalDateTime ahora) {
        validarModelo(modelo);
        validarBodega(bodega);
        return Activo.builder()
                .codigoInventario(normalizarCodigo(datos.codigoInventario()))
                .modelo(modelo)
                .numeroSerie(vacioANulo(datos.numeroSerie()))
                .bodega(bodega)
                .estado(EstadoOperativo.DISPONIBLE)
                .fechaAdquisicion(datos.fechaAdquisicion())
                .costoAdquisicion(datos.costoAdquisicion())
                .observaciones(vacioANulo(datos.observaciones()))
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
    }

    /** Codigo de inventario en mayusculas y sin espacios sobrantes (LED-0001). */
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    /** El codigo de inventario no cambia: identifica al activo en etiquetas e historial. */
    public void actualizar(DatosActivo datos, ModeloActivo nuevoModelo, Bodega nuevaBodega, Integer actor,
                           LocalDateTime ahora) {
        validarModelo(nuevoModelo);
        if (!nuevaBodega.id().equals(bodega.id())) {
            validarBodega(nuevaBodega);
        }
        modelo = nuevoModelo;
        numeroSerie = vacioANulo(datos.numeroSerie());
        bodega = nuevaBodega;
        fechaAdquisicion = datos.fechaAdquisicion();
        costoAdquisicion = datos.costoAdquisicion();
        observaciones = vacioANulo(datos.observaciones());
        registrarModificacion(actor, ahora);
    }

    public void darDeBaja(String motivo, Integer actor, LocalDateTime ahora) {
        if (estaDadoDeBaja()) {
            throw new ReglaNegocioExcepcion("El activo " + codigoInventario + " ya está dado de baja");
        }
        // Un activo en mantenimiento se da de baja al finalizarlo como NO_REPARABLE,
        // para no dejar el mantenimiento abierto.
        if (estado != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioExcepcion(
                    "Solo se puede dar de baja un activo disponible (no reservado, en uso ni en mantenimiento)");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo de la baja");
        }
        estado = EstadoOperativo.BAJA;
        motivoBaja = motivo.trim();
        registrarModificacion(actor, ahora);
    }

    /** Solo un activo disponible puede ingresar a mantenimiento. */
    public void iniciarMantenimiento(Integer actor, LocalDateTime ahora) {
        if (estado != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioExcepcion("Solo un activo disponible puede ingresar a mantenimiento");
        }
        estado = EstadoOperativo.EN_MANTENIMIENTO;
        registrarModificacion(actor, ahora);
    }

    /** Al finalizar vuelve a estar disponible, salvo que no sea reparable. */
    public void finalizarMantenimiento(CondicionFinal condicion, String motivo, Integer actor, LocalDateTime ahora) {
        if (estado != EstadoOperativo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioExcepcion("El activo " + codigoInventario + " no está en mantenimiento");
        }
        if (condicion == CondicionFinal.NO_REPARABLE) {
            estado = EstadoOperativo.BAJA;
            motivoBaja = motivo == null || motivo.isBlank() ? "No reparable tras mantenimiento" : motivo.trim();
        } else {
            estado = EstadoOperativo.DISPONIBLE;
        }
        registrarModificacion(actor, ahora);
    }

    /** El uso historico se registra solo para activos operativos. */
    public void validarPuedeRegistrarUso() {
        if (estaDadoDeBaja() || estado == EstadoOperativo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioExcepcion("No se puede registrar uso de un activo dado de baja o en mantenimiento");
        }
    }

    public void eliminar(Integer actor, LocalDateTime ahora) {
        fechaEliminacion = ahora;
        registrarModificacion(actor, ahora);
    }

    public boolean estaDadoDeBaja() {
        return estado == EstadoOperativo.BAJA;
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validarModelo(ModeloActivo modelo) {
        if (!modelo.controlaPorUnidad()) {
            throw new ReglaNegocioExcepcion("El modelo " + modelo.nombreCompleto()
                    + " se controla por lote: registre sus cantidades en el inventario por lotes");
        }
    }

    private static void validarBodega(Bodega bodega) {
        if (!bodega.activa()) {
            throw new ReglaNegocioExcepcion("La bodega " + bodega.nombre() + " está inactiva");
        }
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
