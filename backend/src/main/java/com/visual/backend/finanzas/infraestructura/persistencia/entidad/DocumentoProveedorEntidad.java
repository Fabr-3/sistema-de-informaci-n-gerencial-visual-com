package com.visual.backend.finanzas.infraestructura.persistencia.entidad;

import java.time.LocalDateTime;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.TipoDocumentoEntidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documentos_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoProveedorEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_proveedor")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false, updatable = false)
    private ProveedorEntidad proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private TipoDocumentoEntidad tipo;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    @Column(name = "modificado_por")
    private Integer modificadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}
