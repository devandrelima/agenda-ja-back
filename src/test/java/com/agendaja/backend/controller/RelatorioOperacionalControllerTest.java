package com.agendaja.backend.controller;

import com.agendaja.backend.dto.RelatorioOperacionalDTO;
import com.agendaja.backend.service.RelatorioOperacionalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioOperacionalControllerTest {
	@Mock private RelatorioOperacionalService relatorioOperacionalService;
	@InjectMocks private RelatorioOperacionalController controller;

	@Test
	void deveDelegarConsultaDoRelatorioOperacionalParaOServico() {
		var inicio = LocalDate.of(2026, 11, 1);
		var fim = LocalDate.of(2026, 11, 30);
		var relatorio = mock(RelatorioOperacionalDTO.class);
		var auth = mock(Authentication.class);

		when(auth.getName()).thenReturn("gestor@teste.com");
		when(relatorioOperacionalService.consultarRelatorio(1L, inicio, fim, "gestor@teste.com"))
				.thenReturn(relatorio);

		var resultado = controller.consultarRelatorio(1L, inicio, fim, auth);

		assertSame(relatorio, resultado);
		verify(relatorioOperacionalService).consultarRelatorio(1L, inicio, fim, "gestor@teste.com");
	}
}
