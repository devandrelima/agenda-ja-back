
package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.service.ServicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.util.List;

@Tag(
		name = "Serviços",
		description = "Gerenciamento dos serviços oferecidos por um estabelecimento"
)
@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/servicos")
@RequiredArgsConstructor
public class ServicoController {

	private final ServicoService servicoService;

	@Operation(
			summary = "Cadastrar serviço",
			description = """
			Cadastra um serviço no estabelecimento informado.
	
			Exige autenticação com papel GESTOR e autorização
			para administrar o estabelecimento.
	
			Requer cookie JSESSIONID e token CSRF válido.
			O campo valor aceita zero, mas não valores negativos.
			"""
	)
	@Parameters({
			@Parameter(
					name = "X-CSRF-TOKEN",
					in = ParameterIn.HEADER,
					required = true,
					description = "Token obtido em GET /api/auth/csrf"
			)
	})
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Serviço cadastrado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado ou CSRF inválido")
	})
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


	@Operation(
			summary = "Listar serviços ativos",
			description = """
			Lista os serviços ativos do estabelecimento informado.
	
			Exige gestor autenticado e autorizado.
			Serviços desativados não são retornados.
			"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de serviços ativos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado")
	})
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


	@Operation(
			summary = "Consultar serviço",
			description = """
			Consulta um serviço ativo pertencente ao estabelecimento.
	
			Exige gestor autenticado e autorizado.
			Serviços inexistentes ou desativados retornam 404.
			"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Serviço encontrado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado"),
			@ApiResponse(responseCode = "404", description = "Serviço não encontrado ou inativo")
	})
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

	@Operation(
			summary = "Editar serviço",
			description = """
			Atualiza nome, duração e preço de um serviço ativo.
	
			Exige gestor autenticado e autorizado.
			Requer cookie JSESSIONID e token CSRF válido.
	
			A edição não modifica o estabelecimento do serviço.
			"""
	)
	@Parameters({
			@Parameter(
					name = "X-CSRF-TOKEN",
					in = ParameterIn.HEADER,
					required = true,
					description = "Token obtido em GET /api/auth/csrf"
			)
	})
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Serviço atualizado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado ou CSRF inválido"),
			@ApiResponse(responseCode = "404", description = "Serviço não encontrado ou inativo")
	})
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

	@Operation(
			summary = "Desativar serviço",
			description = """
			Realiza exclusão lógica (soft delete) do serviço.
	
			O registro permanece no banco de dados, mas deixa
			de aparecer nas consultas de serviços ativos.
	
			Exige gestor autenticado e autorizado.
			Requer cookie JSESSIONID e token CSRF válido.
	
			Não cancela automaticamente agendamentos existentes.
			"""
	)
	@Parameters({
			@Parameter(
					name = "X-CSRF-TOKEN",
					in = ParameterIn.HEADER,
					required = true,
					description = "Token obtido em GET /api/auth/csrf"
			)
	})
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Serviço desativado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado ou CSRF inválido"),
			@ApiResponse(responseCode = "404", description = "Serviço não encontrado ou inativo")
	})
	@DeleteMapping("/{servicoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(
			@PathVariable Long estabelecimentoId,
			@PathVariable Long servicoId,
			Authentication authentication
	) {
		servicoService.desativar(
				estabelecimentoId,
				servicoId,
				authentication.getName()
		);
	}

}
