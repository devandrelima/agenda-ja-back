
package com.agendaja.backend.controller;

import com.agendaja.backend.dto.LoginDTO;
import com.agendaja.backend.dto.SessaoDTO;
import com.agendaja.backend.repository.UsuarioRepository;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final UsuarioRepository usuarioRepository;

	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}

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
