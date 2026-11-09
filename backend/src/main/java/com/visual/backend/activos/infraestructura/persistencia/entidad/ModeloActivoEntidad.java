package com.visual.backend.activos.infraestructura.persistencia.entidad;

import java.util.ArrayList;
import java.util.List;

import com.visual.backend.activos.dominio.modelo.TipoControl;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modelos_activo")
@Getter
@Setter
@NoArgsConstructor
public class ModeloActivoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modelo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_fabricante", nullable = false)
    private FabricanteEntidad fabricante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaEntidad categoria;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_control", nullable = false, length = 10)
    private TipoControl tipoControl;

    @Column(name = "horas_mantenimiento")
    private Integer horasMantenimiento;

    @Column(name = "prefijo_codigo", length = 10)
    private String prefijoCodigo;

    @ElementCollection
    @CollectionTable(name = "especificaciones_modelo", joinColumns = @JoinColumn(name = "id_modelo"))
    @OrderBy("caracteristica")
    private List<EspecificacionEmbebida> especificaciones = new ArrayList<>();
}
