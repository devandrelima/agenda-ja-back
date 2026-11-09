package com.agendaja.backend.service;

import com.agendaja.backend.dto.RedefinicaoSenhaDTO;
import com.agendaja.backend.model.TokenRecuperacaoSenha;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.TokenRecuperacaoSenhaRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

	@Mock
	private UsuarioRepository usuarios;

	@Mock
	private TokenRecuperacaoSenhaRepository tokens;

	@Mock
	private PasswordEncoder encoder;

	@Mock
	private ApplicationEventPublisher eventos;

	@InjectMocks
	private RecuperacaoSenhaService service;

	private Usuario cliente;

	@BeforeEach
	void preparar() {
		ReflectionTestUtils.setField(
				service,
				"urlRedefinicao",
				"http://localhost:5173/redefinir-senha"
		);

		cliente = new Usuario(
				1L,
				null,
				"Maria",
				"maria@teste.com",
				"hash-antigo",
				"CLIENTE"
		);

		cliente.setAtivo(true);
	}

	@Test
	void deveGerarTokenSeguroESalvarApenasHash()
			throws Exception {

		when(usuarios.findByEmail("maria@teste.com"))
				.thenReturn(Optional.of(cliente));

		when(tokens.findTopByUsuarioIdOrderByCriadoEmDesc(1L))
				.thenReturn(Optional.empty());

		service.solicitar(" MARIA@TESTE.COM ");

		var salvo = ArgumentCaptor.forClass(
				TokenRecuperacaoSenha.class
		);

		verify(tokens).save(salvo.capture());

		var evento = ArgumentCaptor.forClass(
				EmailRecuperacaoSenhaEvent.class
		);

		verify(eventos).publishEvent(evento.capture());

		String token = evento.getValue()
				.link()
				.split("token=")[1];

		String hashEsperado = HexFormat.of()
				.formatHex(
						MessageDigest.getInstance("SHA-256")
								.digest(
										token.getBytes(
												StandardCharsets.UTF_8
										)
								)
				);

		assertEquals(
				hashEsperado,
				salvo.getValue().getTokenHash()
		);

		assertNotEquals(
				token,
				salvo.getValue().getTokenHash()
		);

		assertEquals(
				64,
				salvo.getValue().getTokenHash().length()
		);

		assertEquals(
				salvo.getValue().getCriadoEm().plusSeconds(1800),
				salvo.getValue().getExpiraEm()
		);

		verify(tokens).invalidarTodosTokens(
				eq(1L),
				any(Instant.class)
		);
	}

	@Test
	void naoDeveGerarTokenParaEmailInexistente() {
		when(usuarios.findByEmail("ausente@teste.com"))
				.thenReturn(Optional.empty());

		service.solicitar("ausente@teste.com");

		verifyNoInteractions(tokens, eventos);
	}

	@Test
	void naoDeveGerarTokenParaContaInativa() {
		cliente.setAtivo(false);

		when(usuarios.findByEmail("maria@teste.com"))
				.thenReturn(Optional.of(cliente));

		service.solicitar("maria@teste.com");

		verifyNoInteractions(tokens, eventos);
	}

	@Test
	void deveRespeitarIntervaloEntreSolicitacoes() {
		when(usuarios.findByEmail("maria@teste.com"))
				.thenReturn(Optional.of(cliente));

		var anterior = new TokenRecuperacaoSenha();

		anterior.setCriadoEm(
				Instant.now().minusSeconds(10)
		);

		when(tokens.findTopByUsuarioIdOrderByCriadoEmDesc(1L))
				.thenReturn(Optional.of(anterior));

		service.solicitar("maria@teste.com");

		verify(tokens, never()).save(any());
		verifyNoInteractions(eventos);
	}

	@Test
	void deveRedefinirSenhaEConsumirToken() {
		var registro = tokenValido();

		when(tokens.buscarPorHashParaAtualizar(anyString()))
				.thenReturn(Optional.of(registro));

		when(encoder.encode("NovaSenhaForte123"))
				.thenReturn("hash-novo");

		service.redefinir(
				new RedefinicaoSenhaDTO(
						"token-valido",
						"NovaSenhaForte123"
				)
		);

		assertEquals(
				"hash-novo",
				cliente.getSenha()
		);

		assertNotNull(registro.getUtilizadoEm());

		verify(tokens).invalidarOutrosTokens(
				eq(1L),
				eq(8L),
				any(Instant.class)
		);
	}

	@Test
	void naoDeveReutilizarToken() {
		var registro = tokenValido();

		registro.setUtilizadoEm(
				Instant.now().minusSeconds(1)
		);

		when(tokens.buscarPorHashParaAtualizar(anyString()))
				.thenReturn(Optional.of(registro));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.redefinir(
						new RedefinicaoSenhaDTO(
								"token",
								"NovaSenhaForte123"
						)
				)
		);

		assertEquals(
				HttpStatus.BAD_REQUEST,
				erro.getStatusCode()
		);

		verifyNoInteractions(encoder);
	}

	@Test
	void naoDeveAceitarTokenExpirado() {
		var registro = tokenValido();

		registro.setExpiraEm(
				Instant.now().minusSeconds(1)
		);

		when(tokens.buscarPorHashParaAtualizar(anyString()))
				.thenReturn(Optional.of(registro));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.redefinir(
						new RedefinicaoSenhaDTO(
								"token",
								"NovaSenhaForte123"
						)
				)
		);

		assertEquals(
				HttpStatus.BAD_REQUEST,
				erro.getStatusCode()
		);

		verifyNoInteractions(encoder);
	}

	@Test
	void naoDeveAceitarTokenDesconhecido() {
		when(tokens.buscarPorHashParaAtualizar(anyString()))
				.thenReturn(Optional.empty());

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.redefinir(
						new RedefinicaoSenhaDTO(
								"token",
								"NovaSenhaForte123"
						)
				)
		);

		assertEquals(
				HttpStatus.BAD_REQUEST,
				erro.getStatusCode()
		);
	}

	@Test
	void naoDeveRedefinirSenhaDeContaInativa() {
		cliente.setAtivo(false);

		when(tokens.buscarPorHashParaAtualizar(anyString()))
				.thenReturn(
						Optional.of(tokenValido())
				);

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.redefinir(
						new RedefinicaoSenhaDTO(
								"token",
								"NovaSenhaForte123"
						)
				)
		);

		assertEquals(
				HttpStatus.BAD_REQUEST,
				erro.getStatusCode()
		);

		verifyNoInteractions(encoder);
	}

	private TokenRecuperacaoSenha tokenValido() {
		var token = new TokenRecuperacaoSenha();

		token.setId(8L);
		token.setUsuario(cliente);
		token.setCriadoEm(Instant.now());

		token.setExpiraEm(
				Instant.now().plusSeconds(1800)
		);

		return token;
	}
}
