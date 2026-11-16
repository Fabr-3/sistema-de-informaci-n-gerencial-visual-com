package com.visual.backend.activos.dominio.puerto;

import java.util.Optional;

import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.compartido.dominio.Pagina;
import com.visual.backend.compartido.dominio.SolicitudPagina;

public interface RepositorioMantenimientos {

    Pagina<Mantenimiento> listarPorActivo(Integer idActivo, SolicitudPagina solicitud);

    Optional<Mantenimiento> buscarPorId(Integer id);

    Mantenimiento guardar(Mantenimiento mantenimiento);
}
