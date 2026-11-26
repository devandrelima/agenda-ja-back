package com.agendaja.backend.security;

import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsuarioDetailsServiceTest {
	@Test
	void deveLocalizarClientePeloTelefoneFormatado() {
		var repositorio = mock(UsuarioRepository.class);
		var cliente = new Usuario(
				1L, null, "Maria Silva", "maria@email.com", "senha", "CLIENTE"
		);
		cliente.setTelefone("84999999999");
		when(repositorio.findByTelefone("84999999999"))
				.thenReturn(Optional.of(cliente));

		var service = new UsuarioDetailsService(repositorio);
		var detalhes = service.loadUserByUsername("(84) 99999-9999");

		assertEquals("maria@email.com", detalhes.getUsername());
		assertEquals("senha", detalhes.getPassword());
		assertTrue(detalhes.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
		verify(repositorio).findByTelefone("84999999999");
		verify(repositorio, never()).findByEmail(anyString());
	}

	@Test
	void deveLocalizarUsuarioPeloEmailNormalizado() {
		var repositorio = mock(UsuarioRepository.class);
		var gestor = new Usuario(
				2L, null, "Carlos", "gestor@email.com", "hash", "GESTOR"
		);
		when(repositorio.findByEmail("gestor@email.com"))
				.thenReturn(Optional.of(gestor));

		var service = new UsuarioDetailsService(repositorio);
		var detalhes = service.loadUserByUsername("  GESTOR@EMAIL.COM  ");

		assertEquals("gestor@email.com", detalhes.getUsername());
		assertEquals("hash", detalhes.getPassword());
		assertTrue(detalhes.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")));
		verify(repositorio).findByEmail("gestor@email.com");
		verify(repositorio, never()).findByTelefone(anyString());
	}

	@Test
	void deveLancarExcecaoQuandoUsuarioNaoForEncontradoPorEmailOuTelefone() {
		var repositorio = mock(UsuarioRepository.class);
		when(repositorio.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());
		when(repositorio.findByTelefone("84900000000")).thenReturn(Optional.empty());

		var service = new UsuarioDetailsService(repositorio);

		var erroEmail = assertThrows(
				UsernameNotFoundException.class,
				() -> service.loadUserByUsername("inexistente@email.com")
		);
		var erroTelefone = assertThrows(
				UsernameNotFoundException.class,
				() -> service.loadUserByUsername("(84) 90000-0000")
		);

		assertEquals("Usuário não encontrado", erroEmail.getMessage());
		assertEquals("Usuário não encontrado", erroTelefone.getMessage());
	}

	@Test
	void deveBloquearUsuarioComContaDesativada() {
		var repositorio = mock(UsuarioRepository.class);
		var usuarioInativo = new Usuario(
				3L, null, "João", "joao@email.com", "hash", "CLIENTE"
		);
		usuarioInativo.setAtivo(false);
		when(repositorio.findByEmail("joao@email.com"))
				.thenReturn(Optional.of(usuarioInativo));

		var service = new UsuarioDetailsService(repositorio);

		var erro = assertThrows(
				UsernameNotFoundException.class,
				() -> service.loadUserByUsername("joao@email.com")
		);

		assertEquals("Esta conta foi desativada.", erro.getMessage());
	}
}
