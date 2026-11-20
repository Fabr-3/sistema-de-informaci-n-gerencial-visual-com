package com.visual.backend.logistica.dominio.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Reserva por cantidades: el sistema elige las unidades libres de cada
 * modelo. Si algun pedido supera lo disponible no se reserva nada y se
 * informan los faltantes.
 */
public final class ReservaAutomatica {

    private ReservaAutomatica() {
    }

    public static void validar(List<PedidoModelo> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new ReglaNegocioExcepcion("Indique al menos un modelo y su cantidad");
        }
        Set<Integer> modelos = new HashSet<>();
        for (PedidoModelo pedido : pedidos) {
            if (pedido.cantidad() <= 0) {
                throw new ReglaNegocioExcepcion("La cantidad de cada modelo debe ser mayor a cero");
            }
            if (!modelos.add(pedido.idModelo())) {
                throw new ReglaNegocioExcepcion("Cada modelo se indica una sola vez");
            }
        }
    }

    /** Pedidos que superan las unidades libres; un modelo sin unidades libres cuenta con cero. */
    public static List<FaltanteModelo> faltantes(List<PedidoModelo> pedidos, List<DisponibilidadModelo> disponibilidad,
                                                 Function<Integer, String> nombreModelo) {
        Map<Integer, DisponibilidadModelo> porModelo = disponibilidad.stream()
                .collect(Collectors.toMap(DisponibilidadModelo::idModelo, Function.identity()));
        return pedidos.stream().map(pedido -> {
            DisponibilidadModelo libre = porModelo.get(pedido.idModelo());
            int disponibles = libre == null ? 0 : libre.disponibles();
            String modelo = libre == null ? nombreModelo.apply(pedido.idModelo()) : libre.modelo();
            return FaltanteModelo.de(pedido.idModelo(), modelo, pedido.cantidad(), disponibles);
        }).filter(faltante -> faltante.faltante() > 0).toList();
    }
}
