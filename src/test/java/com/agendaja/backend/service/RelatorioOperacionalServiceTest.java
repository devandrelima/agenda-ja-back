package com.agendaja.backend.service;

import com.agendaja.backend.dto.RelatorioOperacionalDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.RelatorioOperacionalProjection;
import com.agendaja.backend.repository.ServicoPopularProjection;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioOperacionalServiceTest {

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@Mock
	private EstabelecimentoAuthorizationService authorizationService;

	@InjectMocks
	private RelatorioOperacionalService relatorioOperacionalService;

	private final Long estabelecimentoId = 1L;
	private final String emailGestor = "gestor@email.com";

	private final LocalDate dataInicio = LocalDate.of(2026, 10, 1);
	private final LocalDate dataFim = LocalDate.of(2026, 10, 31);

	@Test
	void deveConsultarRelatorioComIndicadoresEServicosPopulares() {
		RelatorioOperacionalProjection indicadores =
				mock(RelatorioOperacionalProjection.class);

		when(indicadores.getTotalConcluidos()).thenReturn(42L);
		when(indicadores.getFaturamentoConcluidos())
				.thenReturn(new BigDecimal("3150.00"));
		when(indicadores.getTotalConfirmados()).thenReturn(30L);
		when(indicadores.getTotalConsideradosNaTaxa()).thenReturn(35L);

		when(agendamentoRepository.buscarIndicadoresRelatorio(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(StatusAgendamento.class),
				any(StatusAgendamento.class),
				any(StatusAgendamento.class)
		)).thenReturn(indicadores);

		ServicoPopularProjection servico1 =
				mock(ServicoPopularProjection.class);

		when(servico1.getServicoId()).thenReturn(1L);
		when(servico1.getServicoNome()).thenReturn("Corte de cabelo");
		when(servico1.getQuantidadeAgendamentos()).thenReturn(25L);

		ServicoPopularProjection servico2 =
				mock(ServicoPopularProjection.class);

		when(servico2.getServicoId()).thenReturn(2L);
		when(servico2.getServicoNome()).thenReturn("Barba");
		when(servico2.getQuantidadeAgendamentos()).thenReturn(17L);

		when(agendamentoRepository.buscarServicosMaisPopulares(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(StatusAgendamento.class)
		)).thenReturn(List.of(servico1, servico2));

		RelatorioOperacionalDTO resultado =
				relatorioOperacionalService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		assertEquals(dataInicio, resultado.dataInicio());
		assertEquals(dataFim, resultado.dataFim());
		assertEquals(42L, resultado.totalConcluidos());
		assertEquals(
				new BigDecimal("3150.00"),
				resultado.faturamentoConcluidos()
		);
		assertEquals(
				new BigDecimal("85.71"),
				resultado.taxaConfirmacao()
		);

		assertEquals(2, resultado.servicosMaisPopulares().size());

		assertEquals(
				"Corte de cabelo",
				resultado.servicosMaisPopulares().get(0).servicoNome()
		);
		assertEquals(
				25L,
				resultado.servicosMaisPopulares().get(0).quantidadeAgendamentos()
		);

		assertEquals(
				"Barba",
				resultado.servicosMaisPopulares().get(1).servicoNome()
		);

		verify(authorizationService).validarAcesso(
				emailGestor,
				estabelecimentoId
		);

		verify(agendamentoRepository).buscarIndicadoresRelatorio(
				estabelecimentoId,
				dataInicio.atStartOfDay(),
				dataFim.plusDays(1).atStartOfDay(),
				StatusAgendamento.CONCLUIDO,
				StatusAgendamento.CONFIRMADO,
				StatusAgendamento.CANCELADO
		);

		verify(agendamentoRepository).buscarServicosMaisPopulares(
				estabelecimentoId,
				dataInicio.atStartOfDay(),
				dataFim.plusDays(1).atStartOfDay(),
				StatusAgendamento.CANCELADO
		);
	}

	@Test
	void deveRetornarTaxaZeroQuandoNaoExistiremAgendamentosConsiderados() {
		RelatorioOperacionalProjection indicadores =
				mock(RelatorioOperacionalProjection.class);

		when(indicadores.getTotalConcluidos()).thenReturn(0L);
		when(indicadores.getFaturamentoConcluidos())
				.thenReturn(BigDecimal.ZERO);
		when(indicadores.getTotalConfirmados()).thenReturn(0L);
		when(indicadores.getTotalConsideradosNaTaxa()).thenReturn(0L);

		when(agendamentoRepository.buscarIndicadoresRelatorio(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(StatusAgendamento.class),
				any(StatusAgendamento.class),
				any(StatusAgendamento.class)
		)).thenReturn(indicadores);

		when(agendamentoRepository.buscarServicosMaisPopulares(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(StatusAgendamento.class)
		)).thenReturn(List.of());

		RelatorioOperacionalDTO resultado =
				relatorioOperacionalService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		assertEquals(
				new BigDecimal("0.00"),
				resultado.taxaConfirmacao()
		);
		assertEquals(0L, resultado.totalConcluidos());
		assertTrue(resultado.servicosMaisPopulares().isEmpty());
	}

	@Test
	void deveRetornarErroQuandoDataInicioForPosteriorADataFim() {
		LocalDate inicioInvalido = LocalDate.of(2026, 11, 1);
		LocalDate fimInvalido = LocalDate.of(2026, 10, 1);

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> relatorioOperacionalService.consultarRelatorio(
						estabelecimentoId,
						inicioInvalido,
						fimInvalido,
						emailGestor
				)
		);

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

		verify(authorizationService).validarAcesso(
				emailGestor,
				estabelecimentoId
		);

		verifyNoInteractions(agendamentoRepository);
	}

	@Test
	void deveRetornarErroQuandoDatasForemNulas() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> relatorioOperacionalService.consultarRelatorio(
						estabelecimentoId,
						null,
						dataFim,
						emailGestor
				)
		);

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

		verifyNoInteractions(agendamentoRepository);
	}

	@Test
	void naoDeveConsultarRelatorioQuandoGestorNaoTiverAcesso() {
		doThrow(new AccessDeniedException("Acesso negado"))
				.when(authorizationService)
				.validarAcesso(emailGestor, estabelecimentoId);

		assertThrows(
				AccessDeniedException.class,
				() -> relatorioOperacionalService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				)
		);

		verifyNoInteractions(agendamentoRepository);
	}
}