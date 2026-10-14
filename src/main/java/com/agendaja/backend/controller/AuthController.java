
package com.agendaja.backend.controller;

import com.agendaja.backend.dto.LoginDTO;
import com.agendaja.backend.dto.SessaoDTO;
import com.agendaja.backend.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

@Tag(
		name = "Auth",
		description = "Autenticação"
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final UsuarioRepository usuarioRepository;


	@Operation(
			summary = "Obter token CSRF",
			description = """
				Retorna o token CSRF para operações de escrita.
				O frontend deve preservar o cookie da sessão
				e enviar o token no header indicado pela resposta.
				"""
	)
	@ApiResponse(
			responseCode = "200",
			description = "Token CSRF retornado"
	)
	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}


	@Operation(
			summary = "Autenticar usuário",
			description = """
				Autentica utilizando e-mail e senha.
				Exige token CSRF no header X-CSRF-TOKEN
				e o cookie de sessão correspondente.
				Retorna os dados do usuário autenticado.
				"""
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Login realizado com sucesso"
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados inválidos"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Credenciais inválidas"
			),
			@ApiResponse(
					responseCode = "403",
					description = "Token CSRF ausente ou inválido"
			)
	})
	@Parameter(
			name = "X-CSRF-TOKEN",
			description = "Token obtido em GET /api/auth/csrf",
			required = true,
			in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
	)
	@PostMapping("/login")
	public ResponseEntity<?> login(
			@RequestBody @Valid LoginDTO dto,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		try {
			var credenciais =
					UsernamePasswordAuthenticationToken.unauthenticated(
							dto.email(),
							dto.senha()
					);

			Authentication autenticacao =
					authenticationManager.authenticate(credenciais);

			var contexto = SecurityContextHolder.createEmptyContext();
			contexto.setAuthentication(autenticacao);

			request.getSession(true);
			request.changeSessionId();

			SecurityContextHolder.setContext(contexto);

			securityContextRepository.saveContext(
					contexto, request, response
			);

			return ResponseEntity.ok(
					buscarSessao(autenticacao.getName())
			);

		} catch (AuthenticationException exception) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.body(java.util.Map.of(
							"mensagem", "Credenciais inválidas."
					));
		}
	}


	@Operation(
			summary = "Consultar sessão atual",
			description = """
				Retorna os dados do usuário autenticado.
				Requer cookie de sessão válido.
				"""
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Sessão válida"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Usuário não autenticado"
			)
	})
	@GetMapping("/me")
	public SessaoDTO consultarSessao(Authentication authentication) {
		return buscarSessao(authentication.getName());
	}

	private SessaoDTO buscarSessao(String email) {

		var usuario = usuarioRepository.findByEmail(email)
				.orElseThrow(() ->
						new IllegalStateException(
								"Usuário autenticado não encontrado."
						)
				);

		Long estabelecimentoId = usuario.getEstabelecimento() == null
				? null
				: usuario.getEstabelecimento().getId();

		return new SessaoDTO(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getRole(),
				estabelecimentoId
		);
	}
}
