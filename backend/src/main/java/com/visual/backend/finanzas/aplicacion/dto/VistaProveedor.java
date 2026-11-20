package com.visual.backend.finanzas.aplicacion.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.finanzas.dominio.modelo.DocumentoProveedor;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;

public record VistaProveedor(Integer id, ElementoCatalogo tipo, String razonSocial, String direccion,
                             List<DocumentoProveedor> documentos, String documentoPrincipal, String telefono,
                             String email, List<String> servicios, boolean activo) {

    public static VistaProveedor de(Proveedor proveedor) {
        return new VistaProveedor(proveedor.getId(), proveedor.getTipo(), proveedor.getRazonSocial(),
                proveedor.getDireccion(), proveedor.getDocumentos(), proveedor.documentoPrincipal(),
                proveedor.getTelefono(), proveedor.getEmail(), proveedor.getServicios(), proveedor.isActivo());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("tipo", tipo == null ? null : tipo.nombre());
        mapa.put("razonSocial", razonSocial);
        mapa.put("direccion", direccion);
        mapa.put("documentos", documentos.stream()
                .map(documento -> documento.tipo().nombre() + " " + documento.descripcion()).toList());
        mapa.put("telefono", telefono);
        mapa.put("email", email);
        mapa.put("servicios", servicios);
        mapa.put("activo", activo);
        return mapa;
    }
}
