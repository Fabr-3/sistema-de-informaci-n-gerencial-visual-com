package com.visual.backend.activos.dominio.puerto;

import java.util.List;

import com.visual.backend.activos.dominio.modelo.EntradaHistorialModulo;
import com.visual.backend.activos.dominio.modelo.HistorialModuloLed;

public interface RepositorioHistorialModulos {

    void registrar(HistorialModuloLed cambio);

    /** Cambios del modulo en orden cronologico. */
    List<EntradaHistorialModulo> historial(Integer idModulo);
}
