package com.visual.backend.accesos.infraestructura.persistencia.entidad;

import java.util.HashSet;
import java.util.Set;

import com.visual.backend.accesos.dominio.modelo.CodigoRol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rol fijo: la aplicacion solo modifica su relacion con los permisos. */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class RolEntidad {

    @Id
    @Column(name = "id_rol")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CodigoRol codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Setter
    @ManyToMany
    @JoinTable(name = "roles_permisos",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    private Set<PermisoEntidad> permisos = new HashSet<>();
}
