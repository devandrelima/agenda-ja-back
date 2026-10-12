
package com.agendaja.backend.security;

import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoAuthorizationServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@InjectMocks
	private EstabelecimentoAuthorizationService service;

	private Usuario criarGestor() {
		var estabelecimento = new Estabelecimento(
				1L, "Barbearia Teste", "BARBEARIA", null, "Mossoró/RN"
		);

		return new Usuario(
				1L, estabelecimento, "João Silva",
				"gestor@email.com", "hash", "GESTOR"
		);
	}

	@Test
	void devePermitirGestorDoEstabelecimento() {

		when(usuarioRepository.findByEmail("gestor@email.com"))
				.thenReturn(Optional.of(criarGestor()));

		assertDoesNotThrow(() ->
				service.validarAcesso("gestor@email.com", 1L)
		);
	}

	@Test
	void deveNegarGestorDeOutroEstabelecimento() {

		when(usuarioRepository.findByEmail("gestor@email.com"))
				.thenReturn(Optional.of(criarGestor()));

		assertThrows(
				AccessDeniedException.class,
				() -> service.validarAcesso("gestor@email.com", 2L)
		);
	}
}
