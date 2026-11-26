package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AlterarStatusRequestDTO;
import com.agendaja.backend.dto.ReagendamentoRequestDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.service.AgendamentoGestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgendamentoControllerTest {
	@Mock private AgendamentoGestorService agendamentoGestorService;
	@InjectMocks private AdminAgendamentoController controller;

	private Authentication auth;

	@BeforeEach
	void setup() {
		auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("gestor@teste.com");
	}

	@Test
	void deveDelegarAlteracaoDeStatusParaOServico() {
		var dto = mock(AlterarStatusRequestDTO.class);
		when(dto.status()).thenReturn(StatusAgendamento.CONFIRMADO);

		controller.alterarStatus(1L, 10L, dto, auth);

		verify(agendamentoGestorService).alterarStatus(
				1L, 10L, StatusAgendamento.CONFIRMADO, "gestor@teste.com"
		);
	}

	@Test
	void deveDelegarReagendamentoParaOServico() {
		var novoHorario = LocalDateTime.of(2026, 12, 15, 14, 0);
		var dto = mock(ReagendamentoRequestDTO.class);
		when(dto.dataHoraInicio()).thenReturn(novoHorario);

		controller.reagendar(1L, 10L, dto, auth);

		verify(agendamentoGestorService).reagendar(
				1L, 10L, novoHorario, "gestor@teste.com"
		);
	}
}
