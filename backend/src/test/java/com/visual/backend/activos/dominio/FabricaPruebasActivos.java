package com.visual.backend.activos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.DatosActivo;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.TipoControl;

/** Datos de prueba reutilizables del modulo de activos. */
public final class FabricaPruebasActivos {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 10, 0);
    public static final Bodega BODEGA = new Bodega(1, "Bodega 1", null, true);
    public static final Bodega BODEGA_INACTIVA = new Bodega(3, "Bodega cerrada", null, false);

    private FabricaPruebasActivos() {
    }

    public static ModeloActivo modelo(TipoControl tipoControl, Integer horasMantenimiento) {
        return new ModeloActivo(1, "PL3.9", new Fabricante(1, "Absen"), new Categoria(1, "Pantalla LED"),
                tipoControl, horasMantenimiento, tipoControl == TipoControl.UNIDAD ? "LED" : null, List.of());
    }

    public static Activo activo() {
        return Activo.nuevo(new DatosActivo(" led-001 ", "SN1", LocalDate.of(2025, 1, 1), BigDecimal.TEN, null),
                modelo(TipoControl.UNIDAD, 500), BODEGA, 1, AHORA);
    }
}
