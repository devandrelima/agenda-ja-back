
package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.service.EstabelecimentoService;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EstabelecimentoController.class)
@Import(SecurityConfig.class)
class EstabelecimentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EstabelecimentoService estabelecimentoService;

	private static final String URL = "/api/estabelecimentos";

	@Test
	void deveRetornar201QuandoCadastroForValido() throws Exception {

		String json = """
            {
              "nomeNegocio": "Barbearia Teste",
              "categoria": "BARBEARIA",
              "imagemUrl": null,
              "localizacao": "Mossoró/RN",
              "nomeGestor": "João Silva",
              "email": "joao@email.com",
              "senha": "SenhaSegura123"
            }
            """;

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		verify(estabelecimentoService)
				.cadastrarEstabelecimentoEGestor(any());
	}

	@Test
	void deveRetornar409QuandoEmailJaExiste() throws Exception {

		String json = """
            {
              "nomeNegocio": "Barbearia Teste",
              "categoria": "BARBEARIA",
              "localizacao": "Mossoró/RN",
              "nomeGestor": "João Silva",
              "email": "joao@email.com",
              "senha": "SenhaSegura123"
            }
            """;

		doThrow(new EmailJaCadastradoException(
				"E-mail já cadastrado no sistema."
		)).when(estabelecimentoService)
				.cadastrarEstabelecimentoEGestor(any());

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem")
						.value("E-mail já cadastrado no sistema."));
	}

	@Test
	void deveRetornar400QuandoCamposObrigatoriosEstaoVazios()
			throws Exception {

		String json = """
            {
              "nomeNegocio": "",
              "categoria": "",
              "localizacao": "",
              "nomeGestor": "",
              "email": "",
              "senha": ""
            }
            """;

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem")
						.value("Dados inválidos."))
				.andExpect(jsonPath("$.erros.nomeNegocio").exists())
				.andExpect(jsonPath("$.erros.categoria").exists())
				.andExpect(jsonPath("$.erros.nomeGestor").exists())
				.andExpect(jsonPath("$.erros.email").exists())
				.andExpect(jsonPath("$.erros.senha").exists());

		verifyNoInteractions(estabelecimentoService);
	}

	@Test
	void deveRetornar400QuandoEmailForInvalido() throws Exception {

		String json = """
            {
              "nomeNegocio": "Barbearia Teste",
              "categoria": "BARBEARIA",
              "localizacao": "Mossoró/RN",
              "nomeGestor": "João Silva",
              "email": "email-invalido",
              "senha": "SenhaSegura123"
            }
            """;

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.email").exists());

		verifyNoInteractions(estabelecimentoService);
	}

	@Test
	void deveRemoverEspacosDoEmailAntesDeChamarService()
			throws Exception {

		String json = """
            {
              "nomeNegocio": "Barbearia Teste",
              "categoria": "BARBEARIA",
              "localizacao": "Mossoró/RN",
              "nomeGestor": "João Silva",
              "email": "  joao@email.com  ",
              "senha": "SenhaSegura123"
            }
            """;

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated());

		ArgumentCaptor<EstabelecimentoCadastroDTO> captor =
				ArgumentCaptor.forClass(
						EstabelecimentoCadastroDTO.class
				);

		verify(estabelecimentoService)
				.cadastrarEstabelecimentoEGestor(captor.capture());

		assertEquals(
				"joao@email.com",
				captor.getValue().email()
		);
	}
}
