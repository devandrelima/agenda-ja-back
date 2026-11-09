package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.RedefinicaoSenhaDTO;
import com.agendaja.backend.service.RecuperacaoSenhaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecuperacaoSenhaController.class)
@Import(SecurityConfig.class)
class RecuperacaoSenhaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RecuperacaoSenhaService service;

	@Test
	void devePermitirSolicitarSemAutenticacao()
			throws Exception {

		mvc.perform(
						post("/api/auth/senha/solicitar-recuperacao")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "email": "pessoa@teste.com"
                            }
                        """)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagem").exists());

		verify(service).solicitar("pessoa@teste.com");
	}

	@Test
	void deveExigirCsrf()
			throws Exception {

		mvc.perform(
						post("/api/auth/senha/solicitar-recuperacao")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "email": "pessoa@teste.com"
                            }
                        """)
				)
				.andExpect(status().isForbidden());

		verifyNoInteractions(service);
	}

	@Test
	void deveRedefinirSenhaSemEstarLogado()
			throws Exception {

		mvc.perform(
						post("/api/auth/senha/redefinir")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "token": "token-valido",
                              "novaSenha": "NovaSenhaForte123"
                            }
                        """)
				)
				.andExpect(status().isNoContent());

		verify(service).redefinir(
				any(RedefinicaoSenhaDTO.class)
		);
	}

	@Test
	void deveRetornar400ParaTokenInvalido()
			throws Exception {

		doThrow(
				new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Link inválido."
				)
		).when(service).redefinir(any());

		mvc.perform(
						post("/api/auth/senha/redefinir")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "token": "errado",
                              "novaSenha": "NovaSenhaForte123"
                            }
                        """)
				)
				.andExpect(status().isBadRequest())
				.andExpect(
						jsonPath("$.mensagem")
								.value("Link inválido.")
				);
	}

	@Test
	void deveRejeitarNovaSenhaCurta()
			throws Exception {

		mvc.perform(
						post("/api/auth/senha/redefinir")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "token": "qualquer",
                              "novaSenha": "123"
                            }
                        """)
				)
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}
}
