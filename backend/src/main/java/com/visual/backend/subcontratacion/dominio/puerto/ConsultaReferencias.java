package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.List;
import java.util.Optional;

import com.visual.backend.compartido.dominio.ElementoCatalogo;
import com.visual.backend.subcontratacion.dominio.modelo.ProveedorReferencia;
import com.visual.backend.subcontratacion.dominio.modelo.ProyectoReferencia;

/** Lectura de proyectos, eventos (comercial) y proveedores (finanzas) sin depender de esos modulos. */
public interface ConsultaReferencias {

    Optional<ProyectoReferencia> proyecto(Integer idProyecto);

    /** El evento solo se devuelve si pertenece al proyecto indicado y no esta cancelado. */
    Optional<ElementoCatalogo> eventoDelProyecto(Integer idEvento, Integer idProyecto);

    Optional<ProveedorReferencia> proveedor(Integer idProveedor);

    List<ElementoCatalogo> serviciosDelProveedor(Integer idProveedor);

    /** El servicio solo se devuelve si pertenece al proveedor indicado. */
    Optional<ElementoCatalogo> servicioDelProveedor(Integer idServicio, Integer idProveedor);

    /** Modelo de activos controlado por unidad (los unicos que se reservan y se subalquilan). */
    Optional<ElementoCatalogo> modeloPorUnidad(Integer idModelo);
}
