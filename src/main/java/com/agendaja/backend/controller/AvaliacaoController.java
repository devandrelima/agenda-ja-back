package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AvaliacaoRequestDTO;
import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.service.AvaliacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/avaliacoes")
@RequiredArgsConstructor
@Tag(
		name = "Avaliações - Cliente",
		description = "Publicação e gerenciamento da própria avaliação"
)
public class AvaliacaoController {

	private final AvaliacaoService avaliacaoService;

	@Operation(
			summary = "Publicar avaliação",
			description = """
                    Permite que um cliente autenticado avalie
                    um estabelecimento uma única vez.

                    Exige role CLIENTE e token CSRF válido.
                    """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Avaliação publicada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado"),
			@ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado"),
			@ApiResponse(responseCode = "409", description = "Avaliação já existente")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AvaliacaoResponseDTO publicar(
			@PathVariable Long estabelecimentoId,
			@RequestBody @Valid AvaliacaoRequestDTO dto,
			Authentication authentication
	) {
		return avaliacaoService.publicar(
				estabelecimentoId,
				dto,
				authentication.getName()
		);
	}

	@Operation(
			summary = "Editar minha avaliação",
			description = """
                    Atualiza somente a avaliação do cliente autenticado
                    no estabelecimento informado.
                    Exige role CLIENTE e token CSRF.
                    """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Avaliação atualizada"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado"),
			@ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
	})
	@PutMapping("/minha")
	public AvaliacaoResponseDTO editar(
			@PathVariable Long estabelecimentoId,
			@RequestBody @Valid AvaliacaoRequestDTO dto,
			Authentication authentication
	) {
		return avaliacaoService.editar(
				estabelecimentoId,
				dto,
				authentication.getName()
		);
	}

	@Operation(
			summary = "Excluir minha avaliação",
			description = """
                    Exclui somente a avaliação do cliente autenticado
                    no estabelecimento informado.
                    Exige role CLIENTE e token CSRF.
                    """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Avaliação excluída"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado"),
			@ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
	})
	@DeleteMapping("/minha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(
			@PathVariable Long estabelecimentoId,
			Authentication authentication
	) {
		avaliacaoService.excluir(
				estabelecimentoId,
				authentication.getName()
		);
	}
}
