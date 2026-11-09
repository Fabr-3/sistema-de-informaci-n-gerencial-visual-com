package com.visual.backend.comercial.dominio.puerto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Servicio;

/** Catalogos del modulo: tipos de cliente/documento/contacto, servicios y lugares. */
public interface RepositorioCatalogosComerciales {

    List<ElementoCatalogo> listarTiposCliente();

    List<ElementoCatalogo> listarTiposDocumento();

    List<ElementoCatalogo> listarTiposContacto();

    Optional<ElementoCatalogo> buscarTipoCliente(Integer id);

    Optional<ElementoCatalogo> buscarTipoDocumento(Integer id);

    Optional<ElementoCatalogo> buscarTipoContacto(Integer id);

    List<Servicio> listarServicios(boolean soloActivos);

    List<Servicio> buscarServicios(Collection<Integer> ids);

    boolean existeServicio(String nombre, Integer idExcluido);

    Servicio guardarServicio(Servicio servicio, Integer actor);

    List<Lugar> listarLugares();

    Optional<Lugar> buscarLugar(Integer id);

    boolean existeLugar(String nombre, String direccion, Integer idExcluido);

    Lugar guardarLugar(Lugar lugar, Integer actor);
}
