package com.visual.backend.subcontratacion.infraestructura.seguridad;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.visual.backend.accesos.dominio.modelo.Permisos;
import com.visual.backend.subcontratacion.dominio.puerto.AutorizacionConsumos;

/** Lee los permisos del usuario autenticado en el contexto de Spring Security. */
@Component
public class AutorizacionConsumosSeguridad implements AutorizacionConsumos {

    /** Quien tiene alguno de estos permisos ve y registra consumos de cualquier evento. */
    private static final Set<String> ALCANCE_COMPLETO = Set.of(Permisos.PROVEEDORES_GESTIONAR,
            Permisos.PROVEEDORES_VER);

    @Override
    public boolean limitadoAEventosAsignados() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null || autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).noneMatch(ALCANCE_COMPLETO::contains);
    }
}
