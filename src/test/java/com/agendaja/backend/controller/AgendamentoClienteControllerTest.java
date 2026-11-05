package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.service.AgendamentoClienteService;
import com.agendaja.backend.service.AgendamentoReagendamentoService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
}
