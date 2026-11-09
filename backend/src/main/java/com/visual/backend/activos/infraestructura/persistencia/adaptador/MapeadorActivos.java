package com.visual.backend.activos.infraestructura.persistencia.adaptador;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.Bodega;
import com.visual.backend.activos.dominio.modelo.Categoria;
import com.visual.backend.activos.dominio.modelo.Especificacion;
import com.visual.backend.activos.dominio.modelo.ExistenciaLote;
import com.visual.backend.activos.dominio.modelo.Fabricante;
import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.activos.dominio.modelo.ModeloActivo;
import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.activos.dominio.modelo.MovimientoLote;
import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.activos.dominio.modelo.TipoMantenimiento;
import com.visual.backend.activos.dominio.modelo.UsoActivo;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.BodegaEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.CategoriaEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.FabricanteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.MantenimientoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModeloActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModuloLedEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.MovimientoLoteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.RegistroUsoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.TipoMantenimientoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.jpa.UsoActivoJpaRepositorio.ProyeccionUso;

/** Conversion de entidades JPA a modelos de dominio del modulo de activos. */
final class MapeadorActivos {

    private MapeadorActivos() {
    }

    static Categoria aDominio(CategoriaEntidad entidad) {
        return new Categoria(entidad.getId(), entidad.getNombre());
    }

    static Fabricante aDominio(FabricanteEntidad entidad) {
        return new Fabricante(entidad.getId(), entidad.getNombre());
    }

    static Bodega aDominio(BodegaEntidad entidad) {
        return new Bodega(entidad.getId(), entidad.getNombre(), entidad.getDireccion(), entidad.isActiva());
    }

    static TipoMantenimiento aDominio(TipoMantenimientoEntidad entidad) {
        return new TipoMantenimiento(entidad.getId(), entidad.getNombre(), entidad.isReiniciaHoras());
    }

    static ModeloActivo aDominio(ModeloActivoEntidad entidad) {
        return new ModeloActivo(entidad.getId(), entidad.getNombre(), aDominio(entidad.getFabricante()),
                aDominio(entidad.getCategoria()), entidad.getTipoControl(), entidad.getHorasMantenimiento(), entidad.getPrefijoCodigo(),
                entidad.getEspecificaciones().stream()
                        .map(especificacion -> new Especificacion(especificacion.getCaracteristica(),
                                especificacion.getValor()))
                        .toList());
    }

    static Activo aDominio(ActivoEntidad entidad) {
        return Activo.builder()
                .id(entidad.getId())
                .codigoInventario(entidad.getCodigoInventario())
                .modelo(aDominio(entidad.getModelo()))
                .numeroSerie(entidad.getNumeroSerie())
                .bodega(aDominio(entidad.getBodega()))
                .estado(entidad.getEstado())
                .fechaAdquisicion(entidad.getFechaAdquisicion())
                .costoAdquisicion(entidad.getCostoAdquisicion())
                .observaciones(entidad.getObservaciones())
                .motivoBaja(entidad.getMotivoBaja())
                .creadoPor(entidad.getCreadoPor())
                .modificadoPor(entidad.getModificadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .fechaModificacion(entidad.getFechaModificacion())
                .fechaEliminacion(entidad.getFechaEliminacion())
                .build();
    }

    static UsoActivo aDominio(ProyeccionUso uso) {
        return new UsoActivo(uso.getHorasTotales(), uso.getHorasDesdeMantenimiento(),
                uso.getFechaUltimoMantenimiento());
    }

    static RegistroUso aDominio(RegistroUsoEntidad entidad) {
        return new RegistroUso(entidad.getId(), entidad.getIdActivo(), entidad.getFechaInicio(),
                entidad.getFechaFin(), entidad.getDescripcion(), entidad.getCreadoPor(),
                nombreUsuario(entidad.getCreador()), entidad.getFechaCreacion());
    }

    static Mantenimiento aDominio(MantenimientoEntidad entidad) {
        return Mantenimiento.builder()
                .id(entidad.getId())
                .idActivo(entidad.getIdActivo())
                .tipo(aDominio(entidad.getTipo()))
                .fechaInicio(entidad.getFechaInicio())
                .fechaFin(entidad.getFechaFin())
                .descripcion(entidad.getDescripcion())
                .observaciones(entidad.getObservaciones())
                .condicionFinal(entidad.getCondicionFinal())
                .costo(entidad.getCosto())
                .estado(entidad.getEstado())
                .creadoPor(entidad.getCreadoPor())
                .modificadoPor(entidad.getModificadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    static ModuloLed aDominio(ModuloLedEntidad entidad) {
        return new ModuloLed(entidad.getId(), entidad.getIdActivoPadre(), entidad.getCodigo(), entidad.getEstado(),
                entidad.getFechaCreacion(), entidad.getFechaModificacion());
    }

    static ExistenciaLote aDominio(ExistenciaLoteEntidad entidad) {
        return new ExistenciaLote(entidad.getId(), aDominio(entidad.getModelo()), aDominio(entidad.getBodega()),
                entidad.getCantidad());
    }

    static MovimientoLote aDominio(MovimientoLoteEntidad entidad) {
        return new MovimientoLote(entidad.getId(), entidad.getIdExistencia(), entidad.getTipo(),
                entidad.getCantidad(), entidad.getMotivo(), entidad.getFecha(), entidad.getCreadoPor(),
                nombreUsuario(entidad.getCreador()));
    }

    private static String nombreUsuario(UsuarioEntidad usuario) {
        return usuario == null ? null : usuario.getNombreUsuario();
    }
}
