package com.visual.backend.comercial.aplicacion.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.comercial.dominio.modelo.ResumenComercialCliente;
import com.visual.backend.comercial.dominio.puerto.ConsultaResumenCliente;
import com.visual.backend.comercial.dominio.puerto.RepositorioClientes;

/** Resumen comercial de un cliente (extension de la consulta de clientes). */
@Service
public class ServicioResumenCliente {

    static final int RECIENTES = 5;

    private final RepositorioClientes clientes;
    private final ConsultaResumenCliente consulta;

    public ServicioResumenCliente(RepositorioClientes clientes, ConsultaResumenCliente consulta) {
        this.clientes = clientes;
        this.consulta = consulta;
    }

    @Transactional(readOnly = true)
    public ResumenComercialCliente resumen(Integer idCliente) {
        clientes.obtener(idCliente);
        return consulta.resumen(idCliente, RECIENTES);
    }
}
