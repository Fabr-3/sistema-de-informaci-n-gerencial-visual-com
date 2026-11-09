package com.visual.backend.respaldos.dominio;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Generacion y almacenamiento de copias de seguridad de la base de datos. */
public interface AlmacenRespaldos {

    Respaldo generar();

    /** Respaldos existentes, del mas reciente al mas antiguo. */
    List<Respaldo> listar();

    /** Ruta del respaldo si el nombre es valido y el archivo existe. */
    Optional<Path> ubicar(String nombreArchivo);
}
