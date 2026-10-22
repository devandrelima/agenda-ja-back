
package com.agendaja.backend.controller;

import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.service.ConfiguracaoEstabelecimentoService;
import com.agendaja.backend.dto.HorarioFuncionamentoDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;


@RestController
@RequestMapping("/api/admin/estabelecimentos")
@RequiredArgsConstructor
public class AdminEstabelecimentoController {

	private final EstabelecimentoRepository estabelecimentoRepository;

	private final EstabelecimentoAuthorizationService authorizationService;
	private final ConfiguracaoEstabelecimentoService configuracaoService;

	@Operation(
			summary = "Consultar dados administrativos do estabelecimento",
			description = """
        Retorna os dados do estabelecimento administrado
        pelo gestor autenticado, incluindo seu publicId.

        O publicId pode ser utilizado para construir
        o link público compartilhável com clientes.

        Exige autenticação com papel GESTOR e autorização
        para acessar o estabelecimento informado.
        """
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Estabelecimento encontrado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado"),
			@ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
	})
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
				estabelecimento.getPublicId(),
				estabelecimento.getNome(),
				estabelecimento.getCategoria(),
				estabelecimento.getImagemUrl(),
				estabelecimento.getLocalizacao(),
				configuracaoService.lerHorarios(estabelecimento)
		);
	}

	@PutMapping("/{id}/dados")
	public void atualizarDados(@PathVariable Long id, Authentication authentication,
			@RequestBody DadosRequest dados) {
		configuracaoService.salvarDados(id, authentication.getName(), dados.nome(), dados.categoria(), dados.localizacao());
	}

	@PutMapping("/{id}/capa")
	public void atualizarCapa(@PathVariable Long id, Authentication authentication,
			@RequestBody CapaRequest dados) {
		configuracaoService.salvarCapa(id, authentication.getName(), dados.imagemUrl());
	}

	@PutMapping("/{id}/horarios")
	public void atualizarHorarios(@PathVariable Long id, Authentication authentication,
			@RequestBody List<HorarioFuncionamentoDTO> horarios) {
		configuracaoService.salvarHorarios(id, authentication.getName(), horarios);
	}

	public record DadosRequest(String nome, String categoria, String localizacao) {}
	public record CapaRequest(String imagemUrl) {}

	public record EstabelecimentoResumoDTO(
			Long id,
			UUID publicId,
			String nome,
			String categoria,
			String imagemUrl,
			String localizacao,
			List<HorarioFuncionamentoDTO> horariosFuncionamento
	) {}
}
