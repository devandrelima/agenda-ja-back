package com.agendaja.backend.service;

import com.agendaja.backend.dto.EstabelecimentoPublicoDTO;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.AvaliacaoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstabelecimentoPublicoService {

	private final EstabelecimentoRepository estabelecimentoRepository;
	private final AvaliacaoRepository avaliacaoRepository;
	private final ConfiguracaoEstabelecimentoService configuracaoService;

	@Transactional(readOnly = true)
	public java.util.List<com.agendaja.backend.dto.EstabelecimentoResumoPublicoDTO> listar() {
		return estabelecimentoRepository.findAll().stream()
				.map(estabelecimento -> com.agendaja.backend.dto.EstabelecimentoResumoPublicoDTO.from(
						estabelecimento,
						avaliacaoRepository.calcularMediaPorEstabelecimento(estabelecimento.getId()),
						avaliacaoRepository.countByEstabelecimentoId(estabelecimento.getId())
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public EstabelecimentoPublicoDTO consultar(UUID publicId) {

		var estabelecimento = estabelecimentoRepository
				.findByPublicId(publicId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estabelecimento não encontrado."
				));

		return EstabelecimentoPublicoDTO.from(estabelecimento, configuracaoService.lerHorarios(estabelecimento));
	}
}
