package com.visual.backend.comercial.infraestructura.persistencia.adaptador;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.comercial.dominio.modelo.Lugar;
import com.visual.backend.comercial.dominio.modelo.Servicio;
import com.visual.backend.comercial.dominio.puerto.RepositorioCatalogosComerciales;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.LugarEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.entidad.ServicioEntidad;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.LugarJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.ServicioJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoClienteJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoContactoJpaRepositorio;
import com.visual.backend.comercial.infraestructura.persistencia.jpa.TipoDocumentoJpaRepositorio;

@Repository
public class RepositorioCatalogosComercialesJpa implements RepositorioCatalogosComerciales {

    private final TipoClienteJpaRepositorio tiposCliente;
    private final TipoDocumentoJpaRepositorio tiposDocumento;
    private final TipoContactoJpaRepositorio tiposContacto;
    private final ServicioJpaRepositorio servicios;
    private final LugarJpaRepositorio lugares;
    private final Clock reloj;

    public RepositorioCatalogosComercialesJpa(TipoClienteJpaRepositorio tiposCliente,
                                              TipoDocumentoJpaRepositorio tiposDocumento,
                                              TipoContactoJpaRepositorio tiposContacto,
                                              ServicioJpaRepositorio servicios, LugarJpaRepositorio lugares,
                                              Clock reloj) {
        this.tiposCliente = tiposCliente;
        this.tiposDocumento = tiposDocumento;
        this.tiposContacto = tiposContacto;
        this.servicios = servicios;
        this.lugares = lugares;
        this.reloj = reloj;
    }

    @Override
    public List<ElementoCatalogo> listarTiposCliente() {
        return tiposCliente.findAllByOrderByNombreAsc().stream()
                .map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre())).toList();
    }

    @Override
    public List<ElementoCatalogo> listarTiposDocumento() {
        return tiposDocumento.findAllByOrderByNombreAsc().stream()
                .map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre())).toList();
    }

    @Override
    public List<ElementoCatalogo> listarTiposContacto() {
        return tiposContacto.findAllByOrderByNombreAsc().stream()
                .map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre())).toList();
    }

    @Override
    public Optional<ElementoCatalogo> buscarTipoCliente(Integer id) {
        return tiposCliente.findById(id).map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre()));
    }

    @Override
    public Optional<ElementoCatalogo> buscarTipoDocumento(Integer id) {
        return tiposDocumento.findById(id).map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre()));
    }

    @Override
    public Optional<ElementoCatalogo> buscarTipoContacto(Integer id) {
        return tiposContacto.findById(id).map(tipo -> MapeadorComercial.catalogo(tipo.getId(), tipo.getNombre()));
    }

    @Override
    public List<Servicio> listarServicios(boolean soloActivos) {
        List<ServicioEntidad> encontrados = soloActivos ? servicios.findByActivoTrueOrderByNombreAsc()
                : servicios.findAllByOrderByNombreAsc();
        return encontrados.stream().map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public List<Servicio> buscarServicios(Collection<Integer> ids) {
        return servicios.findAllById(ids).stream().map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public boolean existeServicio(String nombre, Integer idExcluido) {
        return idExcluido == null ? servicios.existsByNombreIgnoreCase(nombre)
                : servicios.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
    }

    @Override
    public Servicio guardarServicio(Servicio servicio, Integer actor) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        ServicioEntidad entidad;
        if (servicio.id() == null) {
            entidad = new ServicioEntidad();
            entidad.setCreadoPor(actor);
            entidad.setFechaCreacion(ahora);
        } else {
            entidad = servicios.getReferenceById(servicio.id());
            entidad.setModificadoPor(actor);
            entidad.setFechaModificacion(ahora);
        }
        entidad.setNombre(servicio.nombre());
        entidad.setDescripcion(servicio.descripcion());
        entidad.setUnidadMedida(servicio.unidadMedida());
        entidad.setPrecioReferencial(servicio.precioReferencial());
        entidad.setActivo(servicio.activo());
        return MapeadorComercial.aDominio(servicios.save(entidad));
    }

    @Override
    public List<Lugar> listarLugares() {
        return lugares.findAllByOrderByNombreAsc().stream().map(MapeadorComercial::aDominio).toList();
    }

    @Override
    public Optional<Lugar> buscarLugar(Integer id) {
        return lugares.findById(id).map(MapeadorComercial::aDominio);
    }

    @Override
    public boolean existeLugar(String nombre, String direccion, Integer idExcluido) {
        return idExcluido == null ? lugares.existsByNombreIgnoreCaseAndDireccionIgnoreCase(nombre, direccion)
                : lugares.existsByNombreIgnoreCaseAndDireccionIgnoreCaseAndIdNot(nombre, direccion, idExcluido);
    }

    @Override
    public Lugar guardarLugar(Lugar lugar, Integer actor) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LugarEntidad entidad;
        if (lugar.id() == null) {
            entidad = new LugarEntidad();
            entidad.setCreadoPor(actor);
            entidad.setFechaCreacion(ahora);
        } else {
            entidad = lugares.getReferenceById(lugar.id());
            entidad.setModificadoPor(actor);
            entidad.setFechaModificacion(ahora);
        }
        entidad.setNombre(lugar.nombre());
        entidad.setDireccion(lugar.direccion());
        entidad.setCiudad(lugar.ciudad());
        return MapeadorComercial.aDominio(lugares.save(entidad));
    }
}
