package com.agendaja.backend.repository;

import com.agendaja.backend.model.Avaliacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository
		extends JpaRepository<Avaliacao, Long> {

	@EntityGraph(attributePaths = "cliente")
	List<Avaliacao> findByEstabelecimentoIdOrderByCriadoEmDesc(
			Long estabelecimentoId
	);

	Optional<Avaliacao> findByClienteIdAndEstabelecimentoId(
			Long clienteId,
			Long estabelecimentoId
	);

	boolean existsByClienteIdAndEstabelecimentoId(
			Long clienteId,
			Long estabelecimentoId
	);

	long countByEstabelecimentoId(Long estabelecimentoId);

	@Query("""
        SELECT AVG(a.nota)
        FROM Avaliacao a
        WHERE a.estabelecimento.id = :estabelecimentoId
        """)
	Double calcularMediaPorEstabelecimento(
			@Param("estabelecimentoId") Long estabelecimentoId
	);
}
