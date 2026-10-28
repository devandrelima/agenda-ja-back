package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.dto.AvaliacaoResumoDTO;
import com.agendaja.backend.service.AvaliacaoPublicaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes")
@RequiredArgsConstructor
@Tag(
		name = "Portal Público - Avaliações",
		description = "Consulta pública de avaliações dos estabelecimentos"
)
public class AvaliacaoPublicaController {

	private final AvaliacaoPublicaService avaliacaoPublicaService;

	@Operation(
			summary = "Listar avaliações públicas",
			description = """
                    Retorna as avaliações publicadas de um estabelecimento,
                    ordenadas da mais recente para a mais antiga.

                    Não exige autenticação nem CSRF.
                    """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Avaliações consultadas com sucesso"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Estabelecimento não encontrado"
			)
	})
	@GetMapping
	public List<AvaliacaoResponseDTO> listar(
			@PathVariable Long estabelecimentoId
	) {
		return avaliacaoPublicaService.listar(estabelecimentoId);
	}

	@Operation(
			summary = "Consultar resumo das avaliações",
			description = """
                    Retorna a quantidade total de avaliações
                    e a média das estrelas de um estabelecimento.

                    Não exige autenticação nem CSRF.
                    """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Resumo consultado com sucesso"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Estabelecimento não encontrado"
			)
	})
	@GetMapping("/resumo")
	public AvaliacaoResumoDTO consultarResumo(
			@PathVariable Long estabelecimentoId
	) {
		return avaliacaoPublicaService.consultarResumo(
				estabelecimentoId
		);
	}
}
