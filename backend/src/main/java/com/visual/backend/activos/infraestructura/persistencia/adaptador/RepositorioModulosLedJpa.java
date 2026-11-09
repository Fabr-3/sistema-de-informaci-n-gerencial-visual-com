package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.activos.dominio.puerto.RepositorioModulosLed;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModuloLedEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.ModuloLedJpaRepositorio;

@Repository
public class RepositorioModulosLedJpa implements RepositorioModulosLed {

    private final ModuloLedJpaRepositorio modulos;

    public RepositorioModulosLedJpa(ModuloLedJpaRepositorio modulos) {
        this.modulos = modulos;
    }

    @Override
    public List<ModuloLed> listarPorActivo(Integer idActivoPadre) {
        return modulos.findByIdActivoPadreAndFechaEliminacionIsNullOrderByCodigoAsc(idActivoPadre).stream()
                .map(MapeadorActivos::aDominio)
                .toList();
    }

    @Override
    public Optional<ModuloLed> buscarPorId(Integer id) {
        return modulos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorActivos::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return modulos.existsByCodigo(codigo);
    }

    @Override
    public ModuloLed guardar(ModuloLed modulo, Integer actor) {
        ModuloLedEntidad entidad;
        if (modulo.getId() == null) {
            entidad = new ModuloLedEntidad();
            entidad.setIdActivoPadre(modulo.getIdActivoPadre());
            entidad.setCodigo(modulo.getCodigo());
            entidad.setCreadoPor(actor);
            entidad.setFechaCreacion(modulo.getFechaCreacion());
        } else {
            entidad = modulos.getReferenceById(modulo.getId());
            entidad.setModificadoPor(actor);
            entidad.setFechaModificacion(modulo.getFechaModificacion());
        }
        entidad.setEstado(modulo.getEstado());
        return MapeadorActivos.aDominio(modulos.save(entidad));
    }
}
