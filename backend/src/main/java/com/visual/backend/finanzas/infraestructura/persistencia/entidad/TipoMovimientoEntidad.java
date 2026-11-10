package com.visual.backend.finanzas.infraestructura.persistencia.entidad;

import com.visual.backend.finanzas.dominio.modelo.Naturaleza;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipos_movimiento")
@Getter
@NoArgsConstructor
public class TipoMovimientoEntidad {

    @Id
    @Column(name = "id_tipo_movimiento")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Naturaleza naturaleza;

    @Column(name = "requiere_proveedor", nullable = false)
    private boolean requiereProveedor;

    @Column(name = "descuenta_inventario", nullable = false)
    private boolean descuentaInventario;
}
