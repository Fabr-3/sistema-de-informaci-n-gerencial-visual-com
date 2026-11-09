package com.visual.backend.comercial.aplicacion.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.Cliente;
import com.visual.backend.comercial.dominio.modelo.ContactoCliente;
import com.visual.backend.comercial.dominio.modelo.DocumentoCliente;
import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;

public record VistaCliente(Integer id, ElementoCatalogo tipo, String razonSocial, String direccion,
                           List<DocumentoCliente> documentos, List<ContactoCliente> contactos,
                           LocalDateTime fechaCreacion) {

    public static VistaCliente de(Cliente cliente) {
        return new VistaCliente(cliente.getId(), cliente.getTipo(), cliente.getRazonSocial(), cliente.getDireccion(),
                cliente.getDocumentos(), cliente.getContactos(), cliente.getFechaCreacion());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("razonSocial", razonSocial);
        mapa.put("tipo", tipo.nombre());
        mapa.put("direccion", direccion);
        mapa.put("documentos", documentos.stream().map(d -> d.tipo().nombre() + " " + d.numero()).toList());
        mapa.put("contactos", contactos.stream().map(c -> c.tipo().nombre() + " " + c.valor()).toList());
        return mapa;
    }
}
