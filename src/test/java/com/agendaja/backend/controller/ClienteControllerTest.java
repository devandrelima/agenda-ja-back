package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.ClienteResponseDTO;
import com.agendaja.backend.service.ClienteService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClienteService clienteService;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	private static final String URL = "/api/clientes";

	private static final String CADASTRO_JSON = """
        {
            "nome": "Maria Silva",
            "telefone": "84999999999",
            "email": "maria@email.com",
            "senha": "SenhaSegura123"
        }
        """;

	@Test
	void devePermitirCadastroPublicoComCsrf() throws Exception {

		when(clienteService.cadastrar(any()))
				.thenReturn(new ClienteResponseDTO(
						1L,
						"Maria Silva",
						"84999999999",
						"maria@email.com"
				));

		mockMvc.perform(post(URL)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(CADASTRO_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Maria Silva"))
				.andExpect(jsonPath("$.telefone").value("84999999999"))
				.andExpect(jsonPath("$.email").value("maria@email.com"))
				.andExpect(jsonPath("$.senha").doesNotExist());

		verify(clienteService).cadastrar(any());
	}

	@Test
	void deveBloquearCadastroSemCsrf() throws Exception {

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(CADASTRO_JSON))
				.andExpect(status().isForbidden());

		verifyNoInteractions(clienteService);
	}

	@Test
	void deveRejeitarCadastroComDadosInvalidos() throws Exception {

		String jsonInvalido = """
            {
                "nome": "",
                "telefone": "123",
                "email": "email-invalido",
                "senha": "123"
            }
            """;

		mockMvc.perform(post(URL)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonInvalido))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(clienteService);
	}
}
