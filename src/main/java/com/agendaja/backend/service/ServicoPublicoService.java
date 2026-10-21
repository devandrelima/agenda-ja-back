package com.agendaja.backend.service;

import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ServicoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoPublicoService {

	private final ServicoRepository servicoRepository;
	private final EstabelecimentoRepository estabelecimentoRepository;

	@Transactional(readOnly = true)
	public List<ServicoResponseDTO> listar(Long estabelecimentoId) {

		validarEstabelecimento(estabelecimentoId);

		return servicoRepository
				.findByEstabelecimentoIdAndAtivoTrue(estabelecimentoId)
				.stream()
				.map(ServicoResponseDTO::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ServicoResponseDTO consultar(
			Long estabelecimentoId,
			Long servicoId
	) {
		validarEstabelecimento(estabelecimentoId);

		var servico = servicoRepository
				.findByIdAndEstabelecimentoIdAndAtivoTrue(
						servicoId,
						estabelecimentoId
				)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Serviço não encontrado."
				));

		return ServicoResponseDTO.from(servico);
	}

	private void validarEstabelecimento(Long estabelecimentoId) {
		if (!estabelecimentoRepository.existsById(estabelecimentoId)) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Estabelecimento não encontrado."
			);
		}
	}
}
