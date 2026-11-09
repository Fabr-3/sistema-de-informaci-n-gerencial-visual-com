package com.visual.backend.accesos.infraestructura.persistencia.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visual.backend.accesos.infraestructura.persistencia.entidad.TokenRefrescoEntidad;

public interface TokenRefrescoJpaRepositorio extends JpaRepository<TokenRefrescoEntidad, Long> {

    Optional<TokenRefrescoEntidad> findByHashToken(String hashToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TokenRefrescoEntidad t set t.fechaRevocacion = :ahora
            where t.idUsuario = :idUsuario and t.fechaRevocacion is null
            """)
    int revocarVigentesDelUsuario(@Param("idUsuario") Integer idUsuario, @Param("ahora") LocalDateTime ahora);
}
