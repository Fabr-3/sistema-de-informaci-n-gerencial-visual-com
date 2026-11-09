package com.visual.backend.activos.dominio.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Modelo de equipo (fabricante + nombre). Determina la categoria, el tipo de
 * control, el intervalo de mantenimiento y el prefijo con el que se generan
 * los codigos de inventario de sus unidades (PREFIJO-0001).
 */
public record ModeloActivo(Integer id, String nombre, Fabricante fabricante, Categoria categoria,
                           TipoControl tipoControl, Integer horasMantenimiento, String prefijoCodigo,
                           List<Especificacion> especificaciones) {

    private static final Pattern FORMATO_PREFIJO = Pattern.compile("^[A-Z0-9]{2,10}$");
    private static final String FORMATO_CODIGO = "%s-%04d";

    public ModeloActivo {
        nombre = nombre == null ? null : nombre.trim();
        prefijoCodigo = normalizarPrefijo(prefijoCodigo);
        especificaciones = especificaciones == null ? List.of() : List.copyOf(especificaciones);
        validar(tipoControl, horasMantenimiento, prefijoCodigo, especificaciones);
    }

    /**
     * Exige el prefijo para registrar unidades. No se valida en el constructor
     * porque los modelos creados antes de existir el prefijo no lo tienen.
     */
    public void validarPrefijoDefinido() {
        if (controlaPorUnidad() && prefijoCodigo == null) {
            throw new ReglaNegocioExcepcion("Defina el prefijo de código del modelo " + nombreCompleto()
                    + " para generar los códigos de sus unidades");
        }
    }

    /** Codigo de inventario de la unidad con el correlativo indicado (NOVA-0007). */
    public String codigoInventario(int correlativo) {
        validarPrefijoDefinido();
        return String.format(FORMATO_CODIGO, prefijoCodigo, correlativo);
    }

    public boolean controlaPorUnidad() {
        return tipoControl == TipoControl.UNIDAD;
    }

    public String nombreCompleto() {
        return fabricante.nombre() + " " + nombre;
    }

    private static String normalizarPrefijo(String prefijo) {
        return prefijo == null || prefijo.isBlank() ? null : prefijo.trim().toUpperCase(Locale.ROOT);
    }

    private static void validar(TipoControl tipoControl, Integer horasMantenimiento, String prefijoCodigo,
                                List<Especificacion> especificaciones) {
        if (tipoControl == TipoControl.LOTE && (horasMantenimiento != null || prefijoCodigo != null)) {
            throw new ReglaNegocioExcepcion(
                    "Los modelos controlados por lote no registran horas de mantenimiento ni prefijo de código");
        }
        if (prefijoCodigo != null && !FORMATO_PREFIJO.matcher(prefijoCodigo).matches()) {
            throw new ReglaNegocioExcepcion("El prefijo de código debe tener de 2 a 10 letras mayúsculas o números");
        }
        if (horasMantenimiento != null && horasMantenimiento <= 0) {
            throw new ReglaNegocioExcepcion("El intervalo de mantenimiento debe ser mayor a cero horas");
        }
        Set<String> caracteristicas = new HashSet<>();
        for (Especificacion especificacion : especificaciones) {
            if (!caracteristicas.add(especificacion.caracteristica().trim().toLowerCase(Locale.ROOT))) {
                throw new ReglaNegocioExcepcion("La característica " + especificacion.caracteristica()
                        + " está repetida");
            }
        }
    }
}
