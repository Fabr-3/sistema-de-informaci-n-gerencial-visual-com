package com.visual.backend.accesos.dominio.modelo;

import java.util.Set;

import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/** Rol fijo del sistema con el conjunto de permisos que otorga. */
public class Rol {

    private final Integer id;
    private final CodigoRol codigo;
    private final String nombre;
    private final String descripcion;
    private Set<Permiso> permisos;

    public Rol(Integer id, CodigoRol codigo, String nombre, String descripcion, Set<Permiso> permisos) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.permisos = Set.copyOf(permisos);
    }

    /**
     * El rol Administrador conserva siempre todos los permisos para evitar
     * que el sistema quede sin nadie capaz de administrarlo.
     */
    public boolean esModificable() {
        return codigo != CodigoRol.ADMINISTRADOR;
    }

    public void reemplazarPermisos(Set<Permiso> nuevosPermisos) {
        if (!esModificable()) {
            throw new ReglaNegocioExcepcion("Los permisos del rol " + nombre + " no se pueden modificar");
        }
        this.permisos = Set.copyOf(nuevosPermisos);
    }

    public boolean esAdministrador() {
        return codigo == CodigoRol.ADMINISTRADOR;
    }

    public Integer getId() {
        return id;
    }

    public CodigoRol getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
