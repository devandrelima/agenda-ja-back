package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AvaliacaoRequestDTO;
import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.service.AvaliacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoControllerTest {
	@Mock private AvaliacaoService avaliacaoService;
	@InjectMocks private AvaliacaoController controller;

	private Authentication auth;

	@BeforeEach
	void setup() {
		auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("cliente@teste.com");
	}

	@Test
	void deveDelegarPublicacaoEdicaoEExclusaoDeAvaliacao() {
		var req = mock(AvaliacaoRequestDTO.class);
		var resp = mock(AvaliacaoResponseDTO.class);

		when(avaliacaoService.publicar(1L, req, "cliente@teste.com")).thenReturn(resp);
		when(avaliacaoService.editar(1L, req, "cliente@teste.com")).thenReturn(resp);

		assertSame(resp, controller.publicar(1L, req, auth));
		assertSame(resp, controller.editar(1L, req, auth));

		controller.excluir(1L, auth);

		verify(avaliacaoService).publicar(1L, req, "cliente@teste.com");
		verify(avaliacaoService).editar(1L, req, "cliente@teste.com");
		verify(avaliacaoService).excluir(1L, "cliente@teste.com");
	}
}
