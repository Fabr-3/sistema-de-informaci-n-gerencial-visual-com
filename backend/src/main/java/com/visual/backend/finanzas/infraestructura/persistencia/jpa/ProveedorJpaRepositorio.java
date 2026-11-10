package com.visual.backend.finanzas.infraestructura.persistencia.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.comercial.infraestructura.persistencia.entidad.TipoDocumentoEntidad;
import com.visual.backend.finanzas.infraestructura.persistencia.entidad.ProveedorEntidad;

public interface ProveedorJpaRepositorio extends JpaRepository<ProveedorEntidad, Integer>,
        JpaSpecificationExecutor<ProveedorEntidad> {

    Optional<ProveedorEntidad> findByIdAndFechaEliminacionIsNull(Integer id);

    @Query("select t from TipoDocumentoEntidad t where t.nombre = :nombre")
    Optional<TipoDocumentoEntidad> buscarTipoDocumento(@Param("nombre") String nombre);

    @Query("""
            select count(d) > 0 from DocumentoProveedorEntidad d
            where d.tipo.nombre = :tipo and d.numero = :numero and d.fechaEliminacion is null
              and d.proveedor.fechaEliminacion is null and (:idExcluido is null or d.proveedor.id <> :idExcluido)
            """)
    boolean existeDocumento(@Param("tipo") String tipo, @Param("numero") String numero,
                            @Param("idExcluido") Integer idExcluido);

    @Query(nativeQuery = true,
            value = "select exists (select 1 from movimientos_financieros where id_proveedor = :idProveedor)")
    boolean tieneMovimientos(@Param("idProveedor") Integer idProveedor);
}
