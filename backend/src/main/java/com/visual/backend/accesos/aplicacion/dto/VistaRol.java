package com.visual.backend.accesos.aplicacion.dto;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.accesos.dominio.modelo.Permiso;
import com.visual.backend.accesos.dominio.modelo.Rol;

public record VistaRol(Integer id, String codigo, String nombre, String descripcion, boolean modificable,
                       List<Permiso> permisos) {

    private static final Comparator<Permiso> ORDEN_PERMISOS =
            Comparator.comparing(Permiso::modulo).thenComparing(Permiso::nombre);

    public static VistaRol de(Rol rol) {
        return new VistaRol(rol.getId(), rol.getCodigo().name(), rol.getNombre(), rol.getDescripcion(),
                rol.esModificable(), rol.getPermisos().stream().sorted(ORDEN_PERMISOS).toList());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("codigo", codigo);
        mapa.put("permisos", permisos.stream().map(Permiso::nombre).toList());
        return mapa;
    }
}
