package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ClienteAtualizacaoDTO;
import com.agendaja.backend.dto.ClienteCadastroDTO;
import com.agendaja.backend.dto.ClienteResponseDTO;
import com.agendaja.backend.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
	private final ClienteService clienteService;

	@Operation(
			summary = "Cadastrar cliente",
			description = """
			Cadastra um novo cliente com nome completo, telefone,
			e-mail e senha.
	
			Endpoint público, sem autenticação.
			Exige token CSRF válido e cookie de sessão correspondente.
	
			O cliente é criado com papel CLIENTE e sem vínculo
			obrigatório com um estabelecimento.
	
			Após o cadastro, o cliente pode se autenticar
			por POST /api/auth/login para confirmar seu agendamento.
			"""
	)
	@Parameter(
			name = "X-CSRF-TOKEN",
			in = ParameterIn.HEADER,
			required = true,
			description = "Token obtido em GET /api/auth/csrf"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Cliente cadastrado"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "403", description = "Token CSRF ausente ou inválido"),
			@ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ClienteResponseDTO cadastrar(
			@RequestBody @Valid ClienteCadastroDTO dto
	) {
		return clienteService.cadastrar(dto);
	}

	@Operation(
            summary = "Consultar meu perfil",
            description = "Retorna os dados do cliente autenticado. Requer papel CLIENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Cliente não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/me")
    public ClienteResponseDTO consultarPerfil(Authentication authentication) {
        return clienteService.consultarPerfil(authentication.getName());
    }

    @Operation(
            summary = "Atualizar meu perfil",
            description = "Atualiza nome, telefone e/e-mail do cliente autenticado. Exige token CSRF."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de formulário inválidos"),
            @ApiResponse(responseCode = "401", description = "Cliente não autenticado"),
            @ApiResponse(responseCode = "403", description = "Token CSRF inválido ou acesso negado"),
            @ApiResponse(responseCode = "409", description = "O novo e-mail já está em uso")
    })
    @PutMapping("/me")
    public ClienteResponseDTO atualizarPerfil(
            @RequestBody @Valid ClienteAtualizacaoDTO dto,
            Authentication authentication
    ) {
        return clienteService.atualizarPerfil(dto, authentication.getName());
    }
}
