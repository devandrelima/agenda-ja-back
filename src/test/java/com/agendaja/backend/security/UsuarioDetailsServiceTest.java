package com.agendaja.backend.security;

import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		verify(repositorio).findByTelefone("84999999999");
		verify(repositorio, never()).findByEmail(anyString());
	}
}
