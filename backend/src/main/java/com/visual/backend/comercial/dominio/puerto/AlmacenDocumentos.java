package com.visual.backend.comercial.dominio.puerto;

import java.util.Optional;

/** Almacenamiento de los documentos firmados que se adjuntan a los registros comerciales. */
public interface AlmacenDocumentos {

    /** Guarda el documento con el nombre indicado; si ya existe, lo reemplaza. */
    void guardar(String nombre, byte[] contenido);

    Optional<byte[]> leer(String nombre);
}
