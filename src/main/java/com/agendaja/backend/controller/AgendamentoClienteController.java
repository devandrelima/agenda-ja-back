package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.service.AgendamentoClienteService;
import com.agendaja.backend.dto.AgendamentoReagendamentoRequestDTO;
import com.agendaja.backend.service.AgendamentoReagendamentoService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

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
	private final AgendamentoReagendamentoService reagendamentoService;

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

	@Operation(
			summary = "Cancelar meu agendamento",
			description = """
        Cancela uma reserva pertencente ao cliente autenticado.

        Permite cancelar reservas pendentes ou confirmadas.
        Não permite cancelar reservas concluídas ou canceladas.

        Exige sessão CLIENTE e token CSRF válido.
        """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Agendamento cancelado"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Cliente não autenticado"
			),
			@ApiResponse(
					responseCode = "403",
					description = "Acesso negado ou CSRF inválido"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Agendamento não encontrado"
			),
			@ApiResponse(
					responseCode = "409",
					description = "Agendamento não pode ser cancelado"
			)
	})
	@PostMapping("/{agendamentoId}/cancelamento")
	public AgendamentoClienteDTO cancelar(
			@PathVariable Long agendamentoId,
			Authentication authentication
	) {
		return service.cancelar(
				agendamentoId,
				authentication.getName()
		);
	}

	@Operation(
			summary = "Reagendar minha reserva",
			description = """
        Reagenda uma reserva pertencente ao cliente autenticado.

        A operação cria uma nova reserva e cancela a anterior
        atomicamente. Se houver erro, a reserva original
        permanece inalterada.

        Exige sessão CLIENTE e token CSRF válido.
        """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "Agendamento reagendado com sucesso"
			),
			@ApiResponse(
					responseCode = "400",
					description = "Dados inválidos"
			),
			@ApiResponse(
					responseCode = "401",
					description = "Cliente não autenticado"
			),
			@ApiResponse(
					responseCode = "403",
					description = "Acesso negado ou CSRF inválido"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Agendamento ou recurso não encontrado"
			),
			@ApiResponse(
					responseCode = "409",
					description = "Reserva indisponível ou status incompatível"
			)
	})
	@PostMapping("/{agendamentoId}/reagendamento")
	@ResponseStatus(HttpStatus.CREATED)
	public AgendamentoClienteDTO reagendar(
			@PathVariable Long agendamentoId,
			@RequestBody @Valid AgendamentoReagendamentoRequestDTO dto,
			Authentication authentication
	) {
		return reagendamentoService.reagendar(
				agendamentoId,
				dto,
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
