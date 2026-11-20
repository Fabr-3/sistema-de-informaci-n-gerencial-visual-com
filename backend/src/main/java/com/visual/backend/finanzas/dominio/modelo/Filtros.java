package com.visual.backend.finanzas.dominio.modelo;

import java.time.LocalDate;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Criterios de busqueda del modulo financiero. */
public final class Filtros {

    private Filtros() {
    }

    /** El texto busca en la razon social, los documentos y los servicios; activo nulo = todos. */
    public record Proveedores(String texto, Boolean activo) {
    }

    /** Fechas incluidas; el texto busca en el concepto y el numero de factura. */
    public record Movimientos(LocalDate desde, LocalDate hasta, Naturaleza naturaleza, Integer idTipo,
                              Integer idProyecto, Integer idEvento, Integer idProveedor, EstadoMovimiento estado,
                              String texto) {
    }

    /**
     * El texto busca en el proyecto y el cliente; el estado es el del proyecto.
     * desde y hasta (incluidos) toman los proyectos cuyo periodo se superpone
     * con el rango; sin orden se listan los mas recientes primero.
     */
    public record ResultadosProyectos(String texto, String estado, LocalDate desde, LocalDate hasta,
                                      Integer idCliente, OrdenResultados orden) {

        public ResultadosProyectos {
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                throw new ReglaNegocioExcepcion("La fecha inicial no puede ser posterior a la final");
            }
            orden = orden == null ? OrdenResultados.FECHA_DESC : orden;
        }

        public static ResultadosProyectos todos() {
            return new ResultadosProyectos(null, null, null, null, null, null);
        }
    }
}
