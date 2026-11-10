package com.visual.backend.finanzas.dominio.modelo;

import java.util.List;

public record DatosProveedor(String razonSocial, String nit, String telefono, String email, List<String> servicios) {
}
