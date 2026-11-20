package com.visual.backend.logistica.dominio.modelo;

/** Pedido que supera las unidades libres del modelo: lo que falta se subalquila o se cancela el evento. */
public record FaltanteModelo(Integer idModelo, String modelo, int solicitada, int disponibles, int faltante) {

    public static FaltanteModelo de(Integer idModelo, String modelo, int solicitada, int disponibles) {
        return new FaltanteModelo(idModelo, modelo, solicitada, disponibles, solicitada - disponibles);
    }
}
