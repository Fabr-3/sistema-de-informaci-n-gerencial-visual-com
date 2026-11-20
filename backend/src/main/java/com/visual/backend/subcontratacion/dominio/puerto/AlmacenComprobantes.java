package com.visual.backend.subcontratacion.dominio.puerto;

import java.util.Optional;

/** Archivos de los comprobantes de gastos, por el nombre generado por el sistema. */
public interface AlmacenComprobantes {

    void guardar(String archivo, byte[] contenido);

    Optional<byte[]> leer(String archivo);

    /** Quita un archivo cuyo registro no llego a guardarse. */
    void eliminar(String archivo);
}
