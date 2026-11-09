package com.visual.backend.logistica.aplicacion.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.logistica.aplicacion.dto.CatalogosLogistica;
import com.visual.backend.logistica.dominio.puerto.RepositorioCatalogosLogistica;

@Service
public class ServicioCatalogosLogistica {

    private final RepositorioCatalogosLogistica catalogos;

    public ServicioCatalogosLogistica(RepositorioCatalogosLogistica catalogos) {
        this.catalogos = catalogos;
    }

    @Transactional(readOnly = true)
    public CatalogosLogistica catalogos() {
        return new CatalogosLogistica(catalogos.rolesEvento(), catalogos.especialidades(),
                catalogos.tiposDocumento(), catalogos.usuariosVinculables());
    }
}
