
package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.service.ServicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/servicos")
@RequiredArgsConstructor
public class ServicoController {

	private final ServicoService servicoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServicoResponseDTO criar(
			@PathVariable Long estabelecimentoId,
			@RequestBody @Valid ServicoRequestDTO dto,
			Authentication authentication
	) {
		return servicoService.criar(
				estabelecimentoId,
				dto,
				authentication.getName()
		);
	}

	@GetMapping
	public List<ServicoResponseDTO> listar(
			@PathVariable Long estabelecimentoId,
			Authentication authentication
	) {
		return servicoService.listar(
				estabelecimentoId,
				authentication.getName()
		);
	}

	@GetMapping("/{servicoId}")
	public ServicoResponseDTO consultar(
			@PathVariable Long estabelecimentoId,
			@PathVariable Long servicoId,
			Authentication authentication
	) {
		return servicoService.consultar(
				estabelecimentoId,
				servicoId,
				authentication.getName()
		);
	}

	@PutMapping("/{servicoId}")
	public ServicoResponseDTO editar(
			@PathVariable Long estabelecimentoId,
			@PathVariable Long servicoId,
			@RequestBody @Valid ServicoRequestDTO dto,
			Authentication authentication
	) {
		return servicoService.editar(
				estabelecimentoId,
				servicoId,
				dto,
				authentication.getName()
		);
	}
}
