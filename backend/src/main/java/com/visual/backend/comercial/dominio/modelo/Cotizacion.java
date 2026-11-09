package com.visual.backend.comercial.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Cotizacion a un cliente. Solo se modifica en borrador; al aceptarla el
 * gerente se convierte en proyecto. El builder se reserva para reconstruirla
 * desde la persistencia (las nuevas usan {@link #nueva}).
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cotizacion {

    private Integer id;
    private String numero;
    private ResumenCliente cliente;
    private String titulo;
    private LocalDate fechaEmision;
    private LocalDate fechaValidez;
    private LocalDate fechaEventoInicio;
    private LocalDate fechaEventoFin;
    private EstadoCotizacion estado;
    private String condiciones;
    private List<LineaCotizacion> lineas;
    private Integer resueltoPor;
    /** Solo lectura: nombre de usuario de quien la acepto o rechazo. */
    private String nombreResolutor;
    private LocalDateTime fechaResolucion;
    private String motivoRechazo;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;

    public static Cotizacion nueva(String numero, DatosCotizacion datos, Integer actor, LocalDateTime ahora) {
        validar(datos);
        Cotizacion cotizacion = Cotizacion.builder()
                .numero(numero)
                .estado(EstadoCotizacion.BORRADOR)
                .creadoPor(actor)
                .fechaCreacion(ahora)
                .build();
        cotizacion.aplicar(datos);
        return cotizacion;
    }

    public void actualizar(DatosCotizacion datos, Integer actor, LocalDateTime ahora) {
        exigirBorrador("modificar");
        validar(datos);
        aplicar(datos);
        registrarModificacion(actor, ahora);
    }

    /** La acepta el gerente: debe seguir vigente y tener importe. */
    public void aceptar(Integer actor, LocalDateTime ahora) {
        exigirBorrador("aceptar");
        if (estaCaducada(ahora.toLocalDate())) {
            throw new ReglaNegocioExcepcion("La cotización " + numero + " venció el " + fechaValidez
                    + "; actualice su fecha de validez antes de aceptarla");
        }
        if (importeTotal().signum() <= 0) {
            throw new ReglaNegocioExcepcion("No se puede aceptar una cotización sin importe");
        }
        resolver(EstadoCotizacion.ACEPTADA, null, actor, ahora);
    }

    public void rechazar(String motivo, Integer actor, LocalDateTime ahora) {
        exigirBorrador("rechazar");
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el motivo del rechazo");
        }
        resolver(EstadoCotizacion.RECHAZADA, motivo.trim(), actor, ahora);
    }

    /** El sistema vence los borradores cuya validez ya paso. Devuelve true si cambio el estado. */
    public boolean vencerSiCorresponde(LocalDate hoy) {
        if (estado != EstadoCotizacion.BORRADOR || !estaCaducada(hoy)) {
            return false;
        }
        estado = EstadoCotizacion.VENCIDA;
        return true;
    }

    /** Solo se eliminan borradores (registros erroneos); las demas se conservan como historial. */
    public void eliminar(Integer actor, LocalDateTime ahora) {
        exigirBorrador("eliminar");
        fechaEliminacion = ahora;
        registrarModificacion(actor, ahora);
    }

    public BigDecimal importeTotal() {
        return lineas.stream().map(LineaCotizacion::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean esBorrador() {
        return estado == EstadoCotizacion.BORRADOR;
    }

    private boolean estaCaducada(LocalDate hoy) {
        return fechaValidez.isBefore(hoy);
    }

    private void aplicar(DatosCotizacion datos) {
        cliente = datos.cliente();
        titulo = datos.titulo().trim();
        fechaEmision = datos.fechaEmision();
        fechaValidez = datos.fechaValidez();
        fechaEventoInicio = datos.fechaEventoInicio();
        fechaEventoFin = datos.fechaEventoFin();
        condiciones = datos.condiciones() == null || datos.condiciones().isBlank() ? null : datos.condiciones().trim();
        lineas = List.copyOf(datos.lineas());
    }

    private void resolver(EstadoCotizacion nuevoEstado, String motivo, Integer actor, LocalDateTime ahora) {
        estado = nuevoEstado;
        motivoRechazo = motivo;
        resueltoPor = actor;
        fechaResolucion = ahora;
        registrarModificacion(actor, ahora);
    }

    private void exigirBorrador(String accion) {
        if (!esBorrador()) {
            throw new ReglaNegocioExcepcion("No se puede " + accion + " la cotización " + numero
                    + " porque ya no está en borrador");
        }
    }

    private void registrarModificacion(Integer actor, LocalDateTime ahora) {
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    private static void validar(DatosCotizacion datos) {
        if (datos.titulo() == null || datos.titulo().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el título de la cotización");
        }
        if (datos.fechaValidez().isBefore(datos.fechaEmision())) {
            throw new ReglaNegocioExcepcion("La fecha de validez no puede ser anterior a la de emisión");
        }
        if (datos.fechaEventoInicio() != null && datos.fechaEventoFin() != null
                && datos.fechaEventoFin().isBefore(datos.fechaEventoInicio())) {
            throw new ReglaNegocioExcepcion("La fecha de fin del evento no puede ser anterior a la de inicio");
        }
        if (datos.lineas() == null || datos.lineas().isEmpty()) {
            throw new ReglaNegocioExcepcion("La cotización debe incluir al menos un servicio");
        }
        Set<Integer> servicios = new HashSet<>();
        for (LineaCotizacion linea : datos.lineas()) {
            if (!servicios.add(linea.idServicio())) {
                throw new ReglaNegocioExcepcion("El servicio " + linea.servicio() + " está repetido en la cotización");
            }
        }
    }
}
