package com.agendaja.backend.service;

import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.dto.AvaliacaoResumoDTO;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvaliacaoPublicaService {

	private final AvaliacaoRepository avaliacaoRepository;
	private final EstabelecimentoRepository estabelecimentoRepository;

	@Transactional(readOnly = true)
	public List<AvaliacaoResponseDTO> listar(Long estabelecimentoId) {

		validarEstabelecimento(estabelecimentoId);

		return avaliacaoRepository
				.findByEstabelecimentoIdOrderByCriadoEmDesc(
						estabelecimentoId
				)
				.stream()
				.map(AvaliacaoResponseDTO::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AvaliacaoResumoDTO consultarResumo(Long estabelecimentoId) {

		validarEstabelecimento(estabelecimentoId);

		long total = avaliacaoRepository
				.countByEstabelecimentoId(estabelecimentoId);

		if (total == 0) {
			return new AvaliacaoResumoDTO(
					estabelecimentoId,
					0,
					BigDecimal.ZERO.setScale(2)
			);
		}

		Double media = avaliacaoRepository
				.calcularMediaPorEstabelecimento(estabelecimentoId);

		BigDecimal mediaArredondada = media == null
				? BigDecimal.ZERO.setScale(2)
				: BigDecimal.valueOf(media)
				.setScale(2, RoundingMode.HALF_UP);

		return new AvaliacaoResumoDTO(
				estabelecimentoId,
				total,
				mediaArredondada
		);
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
