package com.agendaja.backend.controller;

import com.agendaja.backend.dto.RedefinicaoSenhaDTO;
import com.agendaja.backend.dto.SolicitacaoRecuperacaoSenhaDTO;
import com.agendaja.backend.service.RecuperacaoSenhaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/senha")
@RequiredArgsConstructor
@Tag(name = "Auth — recuperação de senha")
public class RecuperacaoSenhaController {

	private final RecuperacaoSenhaService service;

	@PostMapping("/solicitar-recuperacao")
	@Operation(
			summary = "Solicitar e-mail de recuperação de senha"
	)
	@ApiResponse(
			responseCode = "200",
			description = "Resposta genérica, mesmo sem conta cadastrada"
	)
	public Map<String, String> solicitar(
			@RequestBody @Valid
			SolicitacaoRecuperacaoSenhaDTO dto
	) {
		service.solicitar(dto.email());

		return Map.of(
				"mensagem",
				"Se houver uma conta ativa com esse e-mail, "
						+ "você receberá instruções de recuperação."
		);
	}

	@PostMapping("/redefinir")
	@Operation(
			summary = "Redefinir senha com token válido"
	)
	@ApiResponse(
			responseCode = "204",
			description = "Senha redefinida"
	)
	public ResponseEntity<Void> redefinir(
			@RequestBody @Valid RedefinicaoSenhaDTO dto
	) {
		service.redefinir(dto);

		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> tratarStatus(
			ResponseStatusException ex
	) {
		String mensagem = ex.getReason() == null
				? "Solicitação inválida."
				: ex.getReason();

		return ResponseEntity
				.status(ex.getStatusCode())
				.body(Map.of("mensagem", mensagem));
	}
}
