package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ClienteHistoricoDTO;
import com.agendaja.backend.dto.ClienteResumoDTO;
import com.agendaja.backend.service.ClienteGestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminClienteControllerTest {
	@Mock private ClienteGestorService clienteGestorService;
	@InjectMocks private AdminClienteController controller;

	private Authentication auth;

	@BeforeEach
	void setup() {
		auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("gestor@teste.com");
	}

	@Test
	void deveDelegarListagemDeClientesEConsultaDeHistorico() {
		var resumo = mock(ClienteResumoDTO.class);
		var lista = List.of(resumo);
		var historico = mock(ClienteHistoricoDTO.class);

		when(clienteGestorService.listarClientes(1L, "gestor@teste.com")).thenReturn(lista);
		when(clienteGestorService.consultarHistorico(1L, 20L, "gestor@teste.com")).thenReturn(historico);

		assertSame(lista, controller.listarClientes(1L, auth));
		assertSame(historico, controller.consultarHistorico(1L, 20L, auth));

		verify(clienteGestorService).listarClientes(1L, "gestor@teste.com");
		verify(clienteGestorService).consultarHistorico(1L, 20L, "gestor@teste.com");
	}
}
