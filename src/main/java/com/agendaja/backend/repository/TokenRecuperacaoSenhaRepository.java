package com.agendaja.backend.repository;

import com.agendaja.backend.model.TokenRecuperacaoSenha;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository
		extends JpaRepository<TokenRecuperacaoSenha, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
        SELECT t
        FROM TokenRecuperacaoSenha t
        WHERE t.tokenHash = :hash
    """)
	Optional<TokenRecuperacaoSenha> buscarPorHashParaAtualizar(
			@Param("hash") String hash
	);

	Optional<TokenRecuperacaoSenha>
	findTopByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

	@Modifying(flushAutomatically = true)
	@Query("""
        UPDATE TokenRecuperacaoSenha t
        SET t.utilizadoEm = :instante
        WHERE t.usuario.id = :usuarioId
          AND t.utilizadoEm IS NULL
    """)
	int invalidarTodosTokens(
			@Param("usuarioId") Long usuarioId,
			@Param("instante") Instant instante
	);

	@Modifying(flushAutomatically = true)
	@Query("""
        UPDATE TokenRecuperacaoSenha t
        SET t.utilizadoEm = :instante
        WHERE t.usuario.id = :usuarioId
          AND t.utilizadoEm IS NULL
          AND t.id <> :excluirId
    """)
	int invalidarOutrosTokens(
			@Param("usuarioId") Long usuarioId,
			@Param("excluirId") Long excluirId,
			@Param("instante") Instant instante
	);
}
