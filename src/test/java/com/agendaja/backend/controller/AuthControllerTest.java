
package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	private static final String EMAIL = "gestor@email.com";

	private static final String LOGIN_JSON = """
        {
          "email": "gestor@email.com",
          "senha": "SenhaSegura123"
        }
        """;

	private void configurarLoginValido() {

		var autenticacao =
				UsernamePasswordAuthenticationToken.authenticated(
						EMAIL,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_GESTOR"))
				);

		when(authenticationManager.authenticate(any()))
				.thenReturn(autenticacao);

		var usuario = mock(Usuario.class);

		when(usuario.getId()).thenReturn(1L);
		when(usuario.getNome()).thenReturn("João Silva");
		when(usuario.getEmail()).thenReturn(EMAIL);
		when(usuario.getRole()).thenReturn("GESTOR");

		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(usuario));
	}

	private MockHttpSession realizarLogin() throws Exception {
		return (MockHttpSession) mockMvc.perform(
						post("/api/auth/login")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(LOGIN_JSON)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getRequest()
				.getSession(false);
	}

	@Test
	void deveRealizarLoginComCredenciaisValidas()
			throws Exception {

		configurarLoginValido();

		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.role").value("GESTOR"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void deveRejeitarCredenciaisInvalidas()
			throws Exception {

		when(authenticationManager.authenticate(any()))
				.thenThrow(
						new BadCredentialsException("Credenciais inválidas")
				);

		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN_JSON))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(usuarioRepository);
	}

	@Test
	void deveManterAutenticacaoNaSessao()
			throws Exception {

		configurarLoginValido();

		MockHttpSession sessao = realizarLogin();

		mockMvc.perform(get("/api/auth/me")
						.session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.role").value("GESTOR"));
	}

	@Test
	void deveInvalidarSessaoAoFazerLogout()
			throws Exception {

		configurarLoginValido();

		MockHttpSession sessao = realizarLogin();

		mockMvc.perform(post("/api/auth/logout")
						.session(sessao)
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertTrue(sessao.isInvalid());

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveAutenticarClienteEManterSessao() throws Exception {

		String emailCliente = "maria@email.com";

		var autenticacao =
				UsernamePasswordAuthenticationToken.authenticated(
						emailCliente,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
				);

		when(authenticationManager.authenticate(any()))
				.thenReturn(autenticacao);

		var cliente = new Usuario(
				10L,
				null,
				"Maria Silva",
				emailCliente,
				"hash-bcrypt",
				"CLIENTE"
		);

		when(usuarioRepository.findByEmail(emailCliente))
				.thenReturn(Optional.of(cliente));

		String loginJson = """
        {
            "email": "maria@email.com",
            "senha": "SenhaSegura123"
        }
        """;

		var resultado = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.email").value(emailCliente))
				.andExpect(jsonPath("$.role").value("CLIENTE"))
				.andExpect(jsonPath("$.estabelecimentoId").value(
						org.hamcrest.Matchers.nullValue()
				))
				.andReturn();

		MockHttpSession sessao =
				(MockHttpSession) resultado.getRequest().getSession(false);

		assertNotNull(sessao);

		mockMvc.perform(get("/api/auth/me")
						.session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(emailCliente))
				.andExpect(jsonPath("$.role").value("CLIENTE"))
				.andExpect(jsonPath("$.estabelecimentoId").value(
						org.hamcrest.Matchers.nullValue()
				));
	}

}
