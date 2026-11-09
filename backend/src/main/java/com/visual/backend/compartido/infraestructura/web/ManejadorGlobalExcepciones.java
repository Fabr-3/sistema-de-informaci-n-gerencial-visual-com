package com.visual.backend.compartido.infraestructura.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.visual.backend.accesos.dominio.excepcion.AutenticacionExcepcion;
import com.visual.backend.compartido.dominio.excepcion.DatoDuplicadoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.RecursoNoEncontradoExcepcion;
import com.visual.backend.compartido.dominio.excepcion.ReglaNegocioExcepcion;

/**
 * Traduce las excepciones a respuestas RFC 9457 (ProblemDetail). Solo los
 * mensajes de negocio llegan al cliente; los errores inesperados se registran
 * en el log y se responden con un mensaje generico.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String PROPIEDAD_ERRORES = "errores";

    @ExceptionHandler(RecursoNoEncontradoExcepcion.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoExcepcion excepcion) {
        return problema(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(DatoDuplicadoExcepcion.class)
    public ProblemDetail datoDuplicado(DatoDuplicadoExcepcion excepcion) {
        return problema(HttpStatus.CONFLICT, excepcion.getMessage());
    }

    @ExceptionHandler(ReglaNegocioExcepcion.class)
    public ProblemDetail reglaNegocio(ReglaNegocioExcepcion excepcion) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, excepcion.getMessage());
    }

    @ExceptionHandler(AutenticacionExcepcion.class)
    public ProblemDetail autenticacion(AutenticacionExcepcion excepcion) {
        return problema(HttpStatus.UNAUTHORIZED, excepcion.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException excepcion) {
        return problema(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridadDatos(DataIntegrityViolationException excepcion) {
        REGISTRO.warn("Violacion de integridad de datos: {}", excepcion.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception excepcion) {
        REGISTRO.error("Error inesperado", excepcion);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente nuevamente");
    }

    /** Agrega el detalle de cada campo invalido en la propiedad "errores". */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excepcion,
                                                                  HttpHeaders cabeceras, HttpStatusCode estado,
                                                                  WebRequest solicitud) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail detalle = problema(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
        detalle.setProperty(PROPIEDAD_ERRORES, errores);
        return ResponseEntity.badRequest().body(detalle);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException excepcion,
                                                                          HttpHeaders cabeceras,
                                                                          HttpStatusCode estado,
                                                                          WebRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(problema(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el tamaño máximo permitido"));
    }

    private static ProblemDetail problema(HttpStatus estado, String mensaje) {
        return ProblemDetail.forStatusAndDetail(estado, mensaje);
    }
}
