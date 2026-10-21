package com.agendaja.backend.service;

import com.agendaja.backend.dto.EstabelecimentoPublicoDTO;
import com.agendaja.backend.repository.EstabelecimentoRepository;

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

	@Transactional(readOnly = true)
	public EstabelecimentoPublicoDTO consultar(UUID publicId) {

		var estabelecimento = estabelecimentoRepository
				.findByPublicId(publicId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estabelecimento não encontrado."
				));

		return EstabelecimentoPublicoDTO.from(estabelecimento);
	}
}
