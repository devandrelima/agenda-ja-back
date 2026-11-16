
package com.agendaja.backend.service;

import com.agendaja.backend.dto.DashboardResumoDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.DashboardAgendamentoProjection;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@Mock
	private EstabelecimentoAuthorizationService authorizationService;

	@Mock
	private DashboardAgendamentoProjection projection;

	@InjectMocks
	private DashboardService dashboardService;

	private Estabelecimento estabelecimento;
	private Usuario cliente;
	private Profissional profissional;
	private Servico servico;

	private final Long estabelecimentoId = 1L;
	private final String emailGestor = "gestor@teste.com";

	@BeforeEach
	void setup() {
		estabelecimento = new Estabelecimento();
		estabelecimento.setId(estabelecimentoId);

		cliente = new Usuario();
		cliente.setId(10L);
		cliente.setNome("Maria Silva");

		servico = new Servico();
		servico.setId(20L);
		servico.setNome("Corte");
		servico.setValor(new BigDecimal("50.00"));

		profissional = new Profissional();
		profissional.setId(30L);
		profissional.setNome("João");
		profissional.setEstabelecimento(estabelecimento);
	}

	private Agendamento criarAgendamento(
			Long id,
			LocalDateTime horario,
			StatusAgendamento status
	) {
		Agendamento agendamento = new Agendamento();
		agendamento.setId(id);
		agendamento.setCliente(cliente);
		agendamento.setProfissional(profissional);
		agendamento.setServico(servico);
		agendamento.setDataHoraInicio(horario);
		agendamento.setDataHoraFim(horario.plusMinutes(30));
		agendamento.setStatus(status);

		return agendamento;
	}

	private void configurarResumo() {
		when(agendamentoRepository.buscarResumoDashboard(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				any(),
				any(),
				any(),
				any()
		)).thenReturn(projection);

		when(projection.getTotalReservas()).thenReturn(9L);
		when(projection.getConfirmados()).thenReturn(2L);
		when(projection.getPendentes()).thenReturn(5L);
		when(projection.getCancelados()).thenReturn(1L);
		when(projection.getConcluidos()).thenReturn(1L);
		when(projection.getFaturamentoPrevisto())
				.thenReturn(new BigDecimal("400.00"));
	}

	@Test
	void deveRetornarIndicadoresDoDashboard() {
		configurarResumo();

		LocalDate data = LocalDate.now().plusDays(1);

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(List.of());

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						data,
						emailGestor
				);

		assertEquals(data, resultado.data());
		assertEquals(9L, resultado.totalReservas());
		assertEquals(2L, resultado.confirmados());
		assertEquals(5L, resultado.pendentes());
		assertEquals(1L, resultado.cancelados());
		assertEquals(1L, resultado.concluidos());
		assertEquals(
				new BigDecimal("400.00"),
				resultado.faturamentoPrevisto()
		);
	}

	@Test
	void deveRetornarTodosOsProximosAtendimentosSemLimite() {
		configurarResumo();

		LocalDate data = LocalDate.now().plusDays(1);
		LocalDateTime horario = data.atTime(8, 0);

		List<Agendamento> agendamentos = List.of(
				criarAgendamento(1L, horario, StatusAgendamento.CONFIRMADO),
				criarAgendamento(2L, horario.plusHours(1), StatusAgendamento.AGUARDANDO_CONFIRMACAO),
				criarAgendamento(3L, horario.plusHours(2), StatusAgendamento.CONFIRMADO),
				criarAgendamento(4L, horario.plusHours(3), StatusAgendamento.AGUARDANDO_CONFIRMACAO),
				criarAgendamento(5L, horario.plusHours(4), StatusAgendamento.CONFIRMADO),
				criarAgendamento(6L, horario.plusHours(5), StatusAgendamento.AGUARDANDO_CONFIRMACAO)
		);

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(agendamentos);

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						data,
						emailGestor
				);

		assertEquals(6, resultado.proximosAtendimentos().size());
		assertEquals(1L, resultado.proximosAtendimentos().get(0).agendamentoId());
		assertEquals(6L, resultado.proximosAtendimentos().get(5).agendamentoId());
	}

	@Test
	void deveMapearCorretamenteOsDadosDosProximosAtendimentos() {
		configurarResumo();

		LocalDate data = LocalDate.now().plusDays(1);
		LocalDateTime horario = data.atTime(10, 30);

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(List.of(
				criarAgendamento(15L, horario, StatusAgendamento.CONFIRMADO)
		));

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						data,
						emailGestor
				);

		var atendimento = resultado.proximosAtendimentos().get(0);

		assertEquals(15L, atendimento.agendamentoId());
		assertEquals(horario, atendimento.horarioInicio());
		assertEquals("Maria Silva", atendimento.clienteNome());
		assertEquals("Corte", atendimento.servicoNome());
		assertEquals("João", atendimento.profissionalNome());
		assertEquals("CONFIRMADO", atendimento.status());
		assertEquals(new BigDecimal("50.00"), atendimento.valorPrevisto());
	}

	@Test
	void deveUtilizarDataAtualQuandoDataNaoForInformada() {
		configurarResumo();

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(List.of());

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						null,
						emailGestor
				);

		assertEquals(LocalDate.now(), resultado.data());

		verify(agendamentoRepository).buscarResumoDashboard(
				eq(estabelecimentoId),
				eq(LocalDate.now().atStartOfDay()),
				eq(LocalDate.now().plusDays(1).atStartOfDay()),
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	void deveValidarAcessoDoGestorAoEstabelecimento() {
		configurarResumo();

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(List.of());

		dashboardService.consultarDashboard(
				estabelecimentoId,
				LocalDate.now(),
				emailGestor
		);

		verify(authorizationService).validarAcesso(
				emailGestor,
				estabelecimentoId
		);
	}

	@Test
	void deveRetornarListaVaziaQuandoNaoExistiremProximosAtendimentos() {
		configurarResumo();

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(),
				any(LocalDateTime.class),
				any(LocalDateTime.class),
				anyList()
		)).thenReturn(List.of());

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						LocalDate.now().plusDays(1),
						emailGestor
				);

		assertNotNull(resultado.proximosAtendimentos());
		assertTrue(resultado.proximosAtendimentos().isEmpty());
	}
}
