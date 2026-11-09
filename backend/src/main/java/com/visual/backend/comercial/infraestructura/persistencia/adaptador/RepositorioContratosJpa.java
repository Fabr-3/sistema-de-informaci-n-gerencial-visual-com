package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.Contrato;
import com.visual.backend.comercial.dominio.puerto.RepositorioContratos;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ContratoEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ContratoJpaRepositorio;
import com.visual.backend.compartido.infraestructura.persistencia.GeneradorCorrelativos;

@Repository
public class RepositorioContratosJpa implements RepositorioContratos {

    private static final String TABLA = "contratos";
    private static final String COLUMNA_NUMERO = "numero";

    private final ContratoJpaRepositorio contratos;
    private final GeneradorCorrelativos correlativos;

    public RepositorioContratosJpa(ContratoJpaRepositorio contratos, GeneradorCorrelativos correlativos) {
        this.contratos = contratos;
        this.correlativos = correlativos;
    }

    @Override
    public List<Contrato> listarPorProyecto(Integer idProyecto) {
        return contratos.findByIdProyectoAndFechaEliminacionIsNullOrderByFechaFirmaDesc(idProyecto).stream()
                .map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public Optional<Contrato> buscarPorId(Integer id) {
        return contratos.findByIdAndFechaEliminacionIsNull(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public int reservarCorrelativo(String prefijo) {
        return correlativos.reservarUltimo(TABLA, COLUMNA_NUMERO, prefijo);
    }

    @Override
    public Contrato guardar(Contrato contrato) {
        ContratoEntidad entidad = contrato.getId() == null ? new ContratoEntidad()
                : contratos.getReferenceById(contrato.getId());
        entidad.setIdProyecto(contrato.getIdProyecto());
        entidad.setNumero(contrato.getNumero());
        entidad.setFechaFirma(contrato.getFechaFirma());
        entidad.setTerminos(contrato.getTerminos());
        entidad.setArchivoAdjunto(contrato.getArchivoFirmado());
        entidad.setCreadoPor(contrato.getCreadoPor());
        entidad.setFechaCreacion(contrato.getFechaCreacion());
        entidad.setModificadoPor(contrato.getModificadoPor());
        entidad.setFechaModificacion(contrato.getFechaModificacion());
        return MapeadorComercial.aDominio(contratos.save(entidad));
    }
}
