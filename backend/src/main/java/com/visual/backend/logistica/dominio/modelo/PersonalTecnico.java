package com.visual.backend.logistica.dominio.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.visual.backend.comercial.dominio.modelo.ElementoCatalogo;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Persona que trabaja en montajes, operacion y desmontajes. No se elimina:
 * se desactiva para conservar el historial de sus asignaciones.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonalTecnico {

    private Integer id;
    private String nombres;
    private String apellidos;
    private String telefono;
    private DocumentoPersonal documento;
    private List<ElementoCatalogo> especialidades;
    private UsuarioVinculado usuario;
    private boolean activo;
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;

    public static PersonalTecnico nuevo(DatosPersonal datos, Integer actor, LocalDateTime ahora) {
        PersonalTecnico personal = PersonalTecnico.builder().creadoPor(actor).fechaCreacion(ahora).build();
        personal.aplicar(datos);
        return personal;
    }

    public void actualizar(DatosPersonal datos, Integer actor, LocalDateTime ahora) {
        aplicar(datos);
        modificadoPor = actor;
        fechaModificacion = ahora;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public ResumenPersonal resumen() {
        return new ResumenPersonal(id, nombreCompleto());
    }

    public void exigirActivo() {
        if (!activo) {
            throw new ReglaNegocioExcepcion(nombreCompleto() + " está inactivo y no se le puede asignar trabajo");
        }
    }

    private void aplicar(DatosPersonal datos) {
        validar(datos);
        nombres = datos.nombres().trim();
        apellidos = datos.apellidos().trim();
        telefono = datos.telefono() == null || datos.telefono().isBlank() ? null : datos.telefono().trim();
        documento = new DocumentoPersonal(datos.documento().tipo(), datos.documento().numero().trim());
        especialidades = sinRepetir(datos.especialidades());
        usuario = datos.usuario();
        activo = datos.activo();
    }

    private static void validar(DatosPersonal datos) {
        if (datos.nombres() == null || datos.nombres().isBlank()
                || datos.apellidos() == null || datos.apellidos().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique los nombres y apellidos");
        }
        if (datos.documento() == null || datos.documento().numero() == null
                || datos.documento().numero().isBlank()) {
            throw new ReglaNegocioExcepcion("Indique el documento de identidad");
        }
    }

    private static List<ElementoCatalogo> sinRepetir(List<ElementoCatalogo> especialidades) {
        Map<Integer, ElementoCatalogo> unicas = new LinkedHashMap<>();
        if (especialidades != null) {
            especialidades.forEach(especialidad -> unicas.putIfAbsent(especialidad.id(), especialidad));
        }
        return List.copyOf(new ArrayList<>(unicas.values()));
    }
}
