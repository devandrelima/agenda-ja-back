package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.service.ServicoPublicoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/servicos")
@RequiredArgsConstructor
@Tag(
		name = "Portal Público - Serviços",
		description = "Consulta pública dos serviços dos estabelecimentos"
)
public class ServicoPublicoController {

	private final ServicoPublicoService servicoPublicoService;

	@Operation(
			summary = "Listar serviços públicos",
			description = """
            Retorna somente os serviços ativos do estabelecimento,
            incluindo identificador, nome, duração e preço.

            Não exige autenticação nem CSRF.
            """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de serviços ativos"),
			@ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
	})
	@GetMapping
	public List<ServicoResponseDTO> listar(
			@PathVariable Long estabelecimentoId
	) {
		return servicoPublicoService.listar(estabelecimentoId);
	}

	@Operation(
			summary = "Consultar serviço público",
			description = """
            Consulta um serviço ativo que pertença ao
            estabelecimento informado.

            Não exige autenticação nem CSRF.
            """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Serviço encontrado"),
			@ApiResponse(responseCode = "404", description = "Estabelecimento ou serviço não encontrado")
	})
	@GetMapping("/{servicoId}")
	public ServicoResponseDTO consultar(
			@PathVariable Long estabelecimentoId,
			@PathVariable Long servicoId
	) {
		return servicoPublicoService.consultar(
				estabelecimentoId,
				servicoId
		);
	}
}
