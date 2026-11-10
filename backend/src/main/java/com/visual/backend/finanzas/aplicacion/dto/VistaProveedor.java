package com.visual.backend.finanzas.aplicacion.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.finanzas.dominio.modelo.Proveedor;

public record VistaProveedor(Integer id, String razonSocial, String nit, String telefono, String email,
                             List<String> servicios) {

    public static VistaProveedor de(Proveedor proveedor) {
        return new VistaProveedor(proveedor.getId(), proveedor.getRazonSocial(), proveedor.getNit(),
                proveedor.getTelefono(), proveedor.getEmail(), proveedor.getServicios());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("razonSocial", razonSocial);
        mapa.put("nit", nit);
        mapa.put("telefono", telefono);
        mapa.put("email", email);
        mapa.put("servicios", servicios);
        return mapa;
    }
}
