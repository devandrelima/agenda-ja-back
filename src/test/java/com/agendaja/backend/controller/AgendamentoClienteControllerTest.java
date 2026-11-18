package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.dto.AgendamentoReagendamentoRequestDTO;
import com.agendaja.backend.service.AgendamentoClienteService;
import com.agendaja.backend.service.AgendamentoReagendamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgendamentoClienteController.class)
@Import(SecurityConfig.class)
class AgendamentoClienteControllerTest {
	@Autowired
	MockMvc mvc;

	@MockitoBean
	AgendamentoClienteService service;

	@MockitoBean
	private AgendamentoReagendamentoService reagendamentoService;

	@Test
	void devePermitirConsultaAoCliente() throws Exception {
		when(service.listar("cliente@teste.com"))
				.thenReturn(List.of());

		mvc.perform(
						get("/api/clientes/me/agendamentos")
								.with(user("cliente@teste.com")
										.roles("CLIENTE"))
				)
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void deveBloquearConsultaAnonima() throws Exception {
		mvc.perform(
						get("/api/clientes/me/agendamentos")
				)
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveBloquearGestor() throws Exception {
		mvc.perform(
						get("/api/clientes/me/agendamentos")
								.with(user("gestor@teste.com")
										.roles("GESTOR"))
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void deveExigirCsrfNaConfirmacao() throws Exception {
		mvc.perform(
						post("/api/clientes/me/agendamentos/4/confirmacao")
								.with(user("cliente@teste.com")
										.roles("CLIENTE"))
				)
				.andExpect(status().isForbidden());

		verifyNoInteractions(service);
	}

	@Test
	void deveRetornar409ComMensagemDeConflito()
			throws Exception {
		when(service.confirmar(4L, "cliente@teste.com"))
				.thenThrow(new ResponseStatusException(
						HttpStatus.CONFLICT,
						"A reserva não está aguardando confirmação."
				));

		mvc.perform(
						post("/api/clientes/me/agendamentos/4/confirmacao")
								.with(user("cliente@teste.com")
										.roles("CLIENTE"))
								.with(csrf())
				)
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.mensagem").value(
								"A reserva não está aguardando confirmação."
						)
				);
	}

	@Test
	void deveUsarMensagemPadraoQuandoResponseStatusExceptionNaoTiverMotivo() throws Exception {
		when(service.consultar(4L, "cliente@teste.com"))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		mvc.perform(
				get("/api/clientes/me/agendamentos/4")
						.with(user("cliente@teste.com").roles("CLIENTE"))
		)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem")
						.value("Não foi possível processar a solicitação."));
	}

	@Test
	void deveConsultarCancelarEReagendarReservaDoCliente() throws Exception {
		var dto = new AgendamentoClienteDTO(
				4L, 1L, UUID.randomUUID(), "Salão Aurora", "Centro",
				2L, "Corte", 3L, "João",
				LocalDateTime.of(2026, 12, 1, 10, 0),
				LocalDateTime.of(2026, 12, 1, 10, 30),
				"CONFIRMADO", new BigDecimal("50.00")
		);

		when(service.consultar(4L, "cliente@teste.com")).thenReturn(dto);
		when(service.cancelar(4L, "cliente@teste.com")).thenReturn(dto);
		when(reagendamentoService.reagendar(eq(4L), any(AgendamentoReagendamentoRequestDTO.class), eq("cliente@teste.com")))
				.thenReturn(dto);

		mvc.perform(
				get("/api/clientes/me/agendamentos/4")
						.with(user("cliente@teste.com").roles("CLIENTE"))
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4));

		mvc.perform(
				post("/api/clientes/me/agendamentos/4/cancelamento")
						.with(user("cliente@teste.com").roles("CLIENTE"))
						.with(csrf())
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4));

		String reagendamentoJson = """
				{
				  "estabelecimentoId": 1,
				  "servicoId": 2,
				  "profissionalId": 3,
				  "dataHoraInicio": "2026-12-05T14:00:00"
				}
				""";

		mvc.perform(
				post("/api/clientes/me/agendamentos/4/reagendamento")
						.with(user("cliente@teste.com").roles("CLIENTE"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reagendamentoJson)
		)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(4));
	}
}
