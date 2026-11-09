package com.visual.backend.accesos.dominio.puerto;

import com.visual.backend.accesos.dominio.modelo.FiltroAuditoria;
import com.visual.backend.accesos.dominio.modelo.RegistroAuditoria;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

/** Persistencia de auditoria: solo insercion y consulta (registros inmutables). */
public interface RepositorioAuditoria {

    void registrar(RegistroAuditoria registro);

    /** Resultados ordenados del mas reciente al mas antiguo. */
    Pagina<RegistroAuditoria> buscar(FiltroAuditoria filtro, SolicitudPagina solicitud);
}
