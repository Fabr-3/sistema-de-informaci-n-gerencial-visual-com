package com.visual.backend.activos.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.activos.dominio.modelo.Activo;
import com.visual.backend.activos.dominio.modelo.Especificacion;
import com.visual.backend.activos.dominio.modelo.EstadoOperativo;
import com.visual.backend.activos.dominio.modelo.NivelAlertaMantenimiento;
import com.visual.backend.activos.dominio.modelo.UsoActivo;

/** Activo con sus horas de uso y la situacion de su mantenimiento preventivo. */
public record VistaActivo(
        Integer id,
        String codigoInventario,
        String numeroSerie,
        Integer idModelo,
        String modelo,
        Integer idCategoria,
        String categoria,
        Integer idBodega,
        String bodega,
        EstadoOperativo estado,
        LocalDate fechaAdquisicion,
        BigDecimal costoAdquisicion,
        String observaciones,
        String motivoBaja,
        BigDecimal horasTotales,
        BigDecimal horasDesdeMantenimiento,
        Integer horasMantenimiento,
        LocalDateTime fechaUltimoMantenimiento,
        NivelAlertaMantenimiento nivelAlerta,
        List<Especificacion> especificaciones,
        LocalDateTime fechaCreacion) {

    public static VistaActivo de(Activo activo, UsoActivo uso, int porcentajeAlerta) {
        Integer intervalo = activo.getModelo().horasMantenimiento();
        NivelAlertaMantenimiento nivel = activo.estaFueraDeInventario()
                ? NivelAlertaMantenimiento.SIN_PLAN
                : NivelAlertaMantenimiento.calcular(uso.horasDesdeMantenimiento(), intervalo, porcentajeAlerta);
        return new VistaActivo(
                activo.getId(),
                activo.getCodigoInventario(),
                activo.getNumeroSerie(),
                activo.getModelo().id(),
                activo.getModelo().nombreCompleto(),
                activo.getModelo().categoria().id(),
                activo.getModelo().categoria().nombre(),
                activo.getBodega().id(),
                activo.getBodega().nombre(),
                activo.getEstado(),
                activo.getFechaAdquisicion(),
                activo.getCostoAdquisicion(),
                activo.getObservaciones(),
                activo.getMotivoBaja(),
                uso.horasTotales(),
                uso.horasDesdeMantenimiento(),
                intervalo,
                uso.fechaUltimoMantenimiento(),
                nivel,
                activo.getModelo().especificaciones(),
                activo.getFechaCreacion());
    }

    public Map<String, Object> comoMapaAuditoria() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("codigoInventario", codigoInventario);
        mapa.put("numeroSerie", numeroSerie);
        mapa.put("modelo", modelo);
        mapa.put("bodega", bodega);
        mapa.put("estado", estado.name());
        mapa.put("fechaAdquisicion", fechaAdquisicion == null ? null : fechaAdquisicion.toString());
        mapa.put("costoAdquisicion", costoAdquisicion);
        mapa.put("observaciones", observaciones);
        mapa.put("motivoBaja", motivoBaja);
        return mapa;
    }
}
