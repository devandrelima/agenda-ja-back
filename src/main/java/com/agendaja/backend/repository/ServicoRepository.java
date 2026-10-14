
package com.agendaja.backend.repository;

import com.agendaja.backend.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository
		extends JpaRepository<Servico, Long> {

	List<Servico> findByEstabelecimentoId(
			Long estabelecimentoId
	);

	Optional<Servico> findByIdAndEstabelecimentoId(
			Long id,
			Long estabelecimentoId
	);

	List<Servico> findByEstabelecimentoIdAndAtivoTrue(
			Long estabelecimentoId
	);

	Optional<Servico> findByIdAndEstabelecimentoIdAndAtivoTrue(
			Long id,
			Long estabelecimentoId
	);

}
