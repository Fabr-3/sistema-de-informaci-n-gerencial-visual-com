package com.visual.backend.accesos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;
import com.visual.backend.accesos.dominio.modelo.Rol;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

class RolTest {

    @Test
    void elAdministradorNoPuedePerderPermisos() {
        Rol administrador = FabricaPruebasAccesos.rol(CodigoRol.ADMINISTRADOR);

        assertThat(administrador.esModificable()).isFalse();
        assertThatThrownBy(() -> administrador.reemplazarPermisos(Set.of()))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void losDemasRolesPuedenCambiarSusPermisos() {
        Rol gerente = FabricaPruebasAccesos.rol(CodigoRol.GERENTE);

        gerente.reemplazarPermisos(Set.of());

        assertThat(gerente.getPermisos()).isEmpty();
    }
}
