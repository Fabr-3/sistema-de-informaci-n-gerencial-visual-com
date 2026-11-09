package com.visual.backend.respaldos.aplicacion;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visual.backend.accesos.aplicacion.servicio.ServicioAuditoria;
import com.visual.backend.accesos.dominio.modelo.AccionAuditoria;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.respaldos.dominio.AlmacenRespaldos;
import com.visual.backend.respaldos.dominio.Respaldo;

/** Casos de uso de copias de seguridad. Generacion y descarga quedan auditadas. */
@Service
public class ServicioRespaldos {

    public static final String ENTIDAD = "respaldos";

    private final AlmacenRespaldos almacen;
    private final ServicioAuditoria auditoria;

    public ServicioRespaldos(AlmacenRespaldos almacen, ServicioAuditoria auditoria) {
        this.almacen = almacen;
        this.auditoria = auditoria;
    }

    /**
     * Sin transaccion propia: pg_dump puede tardar minutos y no debe retener
     * una conexion; el registro de auditoria se confirma al final.
     */
    public Respaldo generar() {
        Respaldo respaldo = almacen.generar();
        auditoria.registrarOperacion(AccionAuditoria.RESPALDO_CREADO, ENTIDAD, respaldo.nombreArchivo(), null,
                null, "Tamaño: " + respaldo.tamanoBytes() + " bytes");
        return respaldo;
    }

    public List<Respaldo> listar() {
        return almacen.listar();
    }

    @Transactional
    public Path obtenerParaDescarga(String nombreArchivo) {
        Path ruta = almacen.ubicar(nombreArchivo)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("el respaldo", nombreArchivo));
        auditoria.registrarOperacion(AccionAuditoria.RESPALDO_DESCARGADO, ENTIDAD, nombreArchivo, null, null, null);
        return ruta;
    }
}
