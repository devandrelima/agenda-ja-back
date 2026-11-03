package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.service.AgendamentoClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes/me/agendamentos")
@RequiredArgsConstructor
@Tag(
		name = "Meus Agendamentos",
		description = "Consultas e confirmação de reservas do cliente autenticado"
)
public class AgendamentoClienteController {

	private final AgendamentoClienteService service;

	@Operation(
			summary = "Listar meus agendamentos",
			description = """
            Retorna reservas ordenadas por data/hora
            inicial crescente. Requer papel CLIENTE.
            """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Lista de reservas"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Não autenticado"
			),
			@ApiResponse(
					responseCode = "403",
					description = "Não é cliente"
			)
	})
	@GetMapping
	public List<AgendamentoClienteDTO> listar(
			Authentication authentication
	) {
		return service.listar(authentication.getName());
	}

	@Operation(
			summary = "Detalhar meu agendamento",
			description = """
            Retorna 404 para uma reserva inexistente
            ou pertencente a outro cliente.
            """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Detalhes da reserva"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Reserva não encontrada"
			)
	})
	@GetMapping("/{agendamentoId}")
	public AgendamentoClienteDTO consultar(
			@PathVariable Long agendamentoId,
			Authentication authentication
	) {
		return service.consultar(
				agendamentoId,
				authentication.getName()
		);
	}

	@Operation(
			summary = "Confirmar reserva pendente",
			description = """
            Exige cliente autenticado, cookie
            de sessão e token X-CSRF-TOKEN.
            """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Reserva confirmada"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Não autenticado"
			),
			@ApiResponse(
					responseCode = "403",
					description = "Acesso negado ou CSRF inválido"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Reserva não encontrada"
			),
			@ApiResponse(
					responseCode = "409",
					description = "Reserva não está pendente"
			)
	})
	@PostMapping("/{agendamentoId}/confirmacao")
	public AgendamentoClienteDTO confirmar(
			@PathVariable Long agendamentoId,
			Authentication authentication
	) {
		return service.confirmar(
				agendamentoId,
				authentication.getName()
		);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> tratarErro(
			ResponseStatusException exception
	) {
		return ResponseEntity
				.status(exception.getStatusCode())
				.body(Map.of(
						"mensagem",
						exception.getReason() == null
								? "Não foi possível processar a solicitação."
								: exception.getReason()
				));
	}
}
