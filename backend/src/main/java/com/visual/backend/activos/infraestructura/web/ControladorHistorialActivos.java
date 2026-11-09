package com.visual.backend.activos.infraestructura.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.visual.backend.activos.aplicacion.dto.ComandoEstadoModulo;
import com.visual.backend.activos.aplicacion.dto.ComandoFinalizarMantenimiento;
import com.visual.backend.activos.aplicacion.dto.ComandoIniciarMantenimiento;
import com.visual.backend.activos.aplicacion.dto.ComandoModuloLed;
import com.visual.backend.activos.aplicacion.dto.ComandoRegistroUso;
import com.visual.backend.activos.aplicacion.servicio.ServicioMantenimientos;
import com.visual.backend.activos.aplicacion.servicio.ServicioModulosLed;
import com.visual.backend.activos.aplicacion.servicio.ServicioUsoActivos;
import com.visual.backend.activos.dominio.modelo.Mantenimiento;
import com.visual.backend.activos.dominio.modelo.ModuloLed;
import com.visual.backend.activos.dominio.modelo.RegistroUso;
import com.visual.backend.compartido.aplicacion.Pagina;
import com.visual.backend.compartido.aplicacion.SolicitudPagina;

import jakarta.validation.Valid;

/** Historial de un activo: usos, mantenimientos y modulos LED. */
@RestController
@RequestMapping("/api/activos/{idActivo}")
public class ControladorHistorialActivos {

    private final ServicioUsoActivos usos;
    private final ServicioMantenimientos mantenimientos;
    private final ServicioModulosLed modulos;

    public ControladorHistorialActivos(ServicioUsoActivos usos, ServicioMantenimientos mantenimientos,
                                       ServicioModulosLed modulos) {
        this.usos = usos;
        this.mantenimientos = mantenimientos;
        this.modulos = modulos;
    }

    @GetMapping("/usos")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public Pagina<RegistroUso> listarUsos(@PathVariable Integer idActivo,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamano) {
        return usos.listar(idActivo, new SolicitudPagina(pagina, tamano));
    }

    @PostMapping("/usos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.REGISTRAR_MANTENIMIENTO)
    public RegistroUso registrarUso(@PathVariable Integer idActivo,
                                    @Valid @RequestBody ComandoRegistroUso comando) {
        return usos.registrar(idActivo, comando);
    }

    @DeleteMapping("/usos/{idRegistro}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR_O_REGISTRAR)
    public void anularUso(@PathVariable Integer idActivo, @PathVariable Integer idRegistro) {
        usos.anular(idActivo, idRegistro);
    }

    @GetMapping("/mantenimientos")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public Pagina<Mantenimiento> listarMantenimientos(@PathVariable Integer idActivo,
                                                      @RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "20") int tamano) {
        return mantenimientos.listar(idActivo, new SolicitudPagina(pagina, tamano));
    }

    @PostMapping("/mantenimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.REGISTRAR_MANTENIMIENTO)
    public Mantenimiento iniciarMantenimiento(@PathVariable Integer idActivo,
                                              @Valid @RequestBody ComandoIniciarMantenimiento comando) {
        return mantenimientos.iniciar(idActivo, comando);
    }

    @PostMapping("/mantenimientos/{idMantenimiento}/finalizacion")
    @PreAuthorize(ReglasAccesoActivos.REGISTRAR_MANTENIMIENTO)
    public Mantenimiento finalizarMantenimiento(@PathVariable Integer idActivo, @PathVariable Integer idMantenimiento,
                                                @Valid @RequestBody ComandoFinalizarMantenimiento comando) {
        return mantenimientos.finalizar(idActivo, idMantenimiento, comando);
    }

    @GetMapping("/modulos")
    @PreAuthorize(ReglasAccesoActivos.VER)
    public List<ModuloLed> listarModulos(@PathVariable Integer idActivo) {
        return modulos.listar(idActivo);
    }

    @PostMapping("/modulos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR)
    public ModuloLed agregarModulo(@PathVariable Integer idActivo, @Valid @RequestBody ComandoModuloLed comando) {
        return modulos.agregar(idActivo, comando.codigo());
    }

    @PatchMapping("/modulos/{idModulo}/estado")
    @PreAuthorize(ReglasAccesoActivos.GESTIONAR_O_REGISTRAR)
    public ModuloLed cambiarEstadoModulo(@PathVariable Integer idActivo, @PathVariable Integer idModulo,
                                         @Valid @RequestBody ComandoEstadoModulo comando) {
        return modulos.cambiarEstado(idActivo, idModulo, comando.estado());
    }
}
