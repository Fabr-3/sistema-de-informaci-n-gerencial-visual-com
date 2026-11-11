package com.visual.backend.finanzas.infraestructura.persistencia.adaptador;

import java.util.List;

import com.visual.backend.activos.infraestructura.persistencia.entidad.ActivoEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ExistenciaLoteEntidad;
import com.visual.backend.activos.infraestructura.persistencia.entidad.ModeloActivoEntidad;
import com.visual.backend.finanzas.dominio.modelo.ItemVenta;
import com.visual.backend.finanzas.dominio.modelo.MovimientoFinanciero;
import com.visual.backend.finanzas.dominio.modelo.Proveedor;
import com.visual.backend.finanzas.dominio.modelo.Referencia;
import com.visual.backend.finanzas.dominio.modelo.TipoMovimiento;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.MovimientoFinancieroEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.TipoMovimientoEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.VentaDetalleEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.DocumentoProveedorEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;

/** Conversion de entidades JPA a modelos de dominio del modulo financiero. */
final class MapeadorFinanzas {

    private MapeadorFinanzas() {
    }

    static Proveedor aDominio(ProveedorEntidad entidad) {
        DocumentoProveedorEntidad nit = documentoNit(entidad);
        return Proveedor.builder()
                .id(entidad.getId())
                .razonSocial(entidad.getRazonSocial())
                .nit(nit == null ? null : nit.getNumero())
                .telefono(entidad.getTelefono())
                .email(entidad.getEmail())
                .servicios(List.copyOf(entidad.getServicios()))
                .creadoPor(entidad.getCreadoPor())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .fechaEliminacion(entidad.getFechaEliminacion())
                .build();
    }

    static TipoMovimiento aDominio(TipoMovimientoEntidad entidad) {
        return new TipoMovimiento(entidad.getId(), entidad.getNombre(), entidad.getNaturaleza(),
                entidad.isRequiereProveedor(), entidad.isDescuentaInventario(), entidad.getCodigoSistema());
    }

    static MovimientoFinanciero aDominio(MovimientoFinancieroEntidad entidad) {
        return MovimientoFinanciero.builder()
                .id(entidad.getId())
                .tipo(aDominio(entidad.getTipo()))
                .concepto(entidad.getConcepto())
                .monto(entidad.getMonto())
                .fecha(entidad.getFecha())
                .proyecto(entidad.getProyecto() == null ? null
                        : new Referencia(entidad.getProyecto().getId(), entidad.getProyecto().getNombre()))
                .evento(entidad.getEvento() == null ? null
                        : new Referencia(entidad.getEvento().getId(), entidad.getEvento().getNombre()))
                .proveedor(entidad.getProveedor() == null ? null
                        : new Referencia(entidad.getProveedor().getId(), entidad.getProveedor().getRazonSocial()))
                .numeroFactura(entidad.getNumeroFactura())
                .estado(entidad.getEstado())
                .motivoAnulacion(entidad.getMotivoAnulacion())
                .items(entidad.getDetalle().stream().map(MapeadorFinanzas::item).toList())
                .idMantenimiento(entidad.getIdMantenimiento())
                .creadoPor(entidad.getCreadoPor())
                .nombreCreador(entidad.getCreador() == null ? null : entidad.getCreador().nombreCompleto())
                .fechaCreacion(entidad.getFechaCreacion())
                .modificadoPor(entidad.getModificadoPor())
                .fechaModificacion(entidad.getFechaModificacion())
                .build();
    }

    /** "ABPL-0003 · Absen PL3.9" o "Cable XLR 10 m (Bodega 1)". */
    private static ItemVenta item(VentaDetalleEntidad detalle) {
        ActivoEntidad activo = detalle.getActivo();
        if (activo != null) {
            return ItemVenta.unidad(activo.getId(), activo.getCodigoInventario() + " · " + modelo(activo.getModelo()));
        }
        ExistenciaLoteEntidad existencia = detalle.getExistencia();
        return ItemVenta.lote(existencia.getId(), detalle.getCantidad(),
                modelo(existencia.getModelo()) + " (" + existencia.getBodega().getNombre() + ")");
    }

    private static String modelo(ModeloActivoEntidad modelo) {
        return modelo.getFabricante().getNombre() + " " + modelo.getNombre();
    }

    static DocumentoProveedorEntidad documentoNit(ProveedorEntidad entidad) {
        return entidad.getDocumentos().stream()
                .filter(documento -> documento.getFechaEliminacion() == null
                        && RepositorioProveedoresJpa.TIPO_NIT.equals(documento.getTipo().getNombre()))
                .findFirst().orElse(null);
    }
}
