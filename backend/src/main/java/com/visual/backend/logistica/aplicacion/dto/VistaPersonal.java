package com.visual.backend.logistica.aplicacion.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.logistica.dominio.modelo.DocumentoPersonal;
import com.visual.backend.logistica.dominio.modelo.PersonalTecnico;
import com.visual.backend.logistica.dominio.modelo.UsuarioVinculado;

public record VistaPersonal(Integer id, String nombres, String apellidos, String nombreCompleto, String telefono,
                            DocumentoPersonal documento, List<ElementoCatalogo> especialidades,
                            UsuarioVinculado usuario, boolean activo) {

    public static VistaPersonal de(PersonalTecnico personal) {
        return new VistaPersonal(personal.getId(), personal.getNombres(), personal.getApellidos(),
                personal.nombreCompleto(), personal.getTelefono(), personal.getDocumento(),
                personal.getEspecialidades(), personal.getUsuario(), personal.isActivo());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", nombreCompleto);
        mapa.put("telefono", telefono);
        mapa.put("documento", documento.tipo().nombre() + " " + documento.numero());
        mapa.put("especialidades", especialidades.stream().map(ElementoCatalogo::nombre).toList());
        mapa.put("usuario", usuario == null ? null : usuario.nombreUsuario());
        mapa.put("activo", activo);
        return mapa;
    }
}
