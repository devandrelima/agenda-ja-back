
package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminEstabelecimentoController.class)
@Import(SecurityConfig.class)
class AdminEstabelecimentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EstabelecimentoRepository estabelecimentoRepository;

	@MockitoBean
	private EstabelecimentoAuthorizationService authorizationService;

	private static final String URL =
			"/api/admin/estabelecimentos/1";

	@Test
	void deveNegarAcessoSemLogin() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveNegarAcessoParaCliente() throws Exception {
		mockMvc.perform(get(URL)
						.with(user("cliente@email.com").roles("CLIENTE")))
				.andExpect(status().isForbidden());
	}

	@Test
	void devePermitirAcessoAoProprioEstabelecimento()
			throws Exception {

		UUID publicId = UUID.randomUUID();

		var estabelecimento = new Estabelecimento(
				1L,
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		estabelecimento.setPublicId(publicId);

		when(estabelecimentoRepository.findById(1L))
				.thenReturn(Optional.of(estabelecimento));

		mockMvc.perform(get(URL)
						.with(user("gestor@email.com").roles("GESTOR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.publicId").value(publicId.toString()))
				.andExpect(jsonPath("$.nome").value("Barbearia Teste"));

		verify(authorizationService)
				.validarAcesso("gestor@email.com", 1L);
	}

	@Test
	void deveNegarAcessoAOutroEstabelecimento()
			throws Exception {

		doThrow(new AccessDeniedException("Acesso negado."))
				.when(authorizationService)
				.validarAcesso("gestor@email.com", 1L);

		mockMvc.perform(get(URL)
						.with(user("gestor@email.com").roles("GESTOR")))
				.andExpect(status().isForbidden());

		verify(estabelecimentoRepository, never())
				.findById(anyLong());
	}
}
