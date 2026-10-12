
package com.agendaja.backend.controller;

import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/estabelecimentos")
@RequiredArgsConstructor
public class AdminEstabelecimentoController {

	private final EstabelecimentoRepository estabelecimentoRepository;

	private final EstabelecimentoAuthorizationService authorizationService;

	@GetMapping("/{id}")
	public EstabelecimentoResumoDTO consultar(
			@PathVariable Long id,
			Authentication authentication
	) {
		authorizationService.validarAcesso(
				authentication.getName(),
				id
		);

		var estabelecimento = estabelecimentoRepository.findById(id)
				.orElseThrow(() ->
						new ResponseStatusException(
								HttpStatus.NOT_FOUND,
								"Estabelecimento não encontrado."
						)
				);

		return new EstabelecimentoResumoDTO(
				estabelecimento.getId(),
				estabelecimento.getNome(),
				estabelecimento.getCategoria(),
				estabelecimento.getImagemUrl(),
				estabelecimento.getLocalizacao()
		);
	}

	public record EstabelecimentoResumoDTO(
			Long id,
			String nome,
			String categoria,
			String imagemUrl,
			String localizacao
	) {}
}
