package com.agendaja.backend.service;

import com.agendaja.backend.config.TimeConfig;
import com.agendaja.backend.dto.DashboardResumoDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.DashboardAgendamentoProjection;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Clock;
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

	private Clock clockFixo;
	private LocalDate hojeFixo;
	private LocalDateTime agoraFixo;
	private DashboardService dashboardService;

	private Estabelecimento estabelecimento;
	private Usuario cliente;
	private Profissional profissional;
	private Servico servico;

	private final Long estabelecimentoId = 1L;
	private final String emailGestor = "gestor@teste.com";

	@BeforeEach
	void setup() {
		// Fixa "agora" em 25/11/2026 às 14:00 (America/Sao_Paulo = 17:00 UTC)
		agoraFixo = LocalDateTime.of(2026, 11, 25, 14, 0);
		hojeFixo = agoraFixo.toLocalDate();
		clockFixo = Clock.fixed(
				agoraFixo.atZone(TimeConfig.ZONA_PADRAO).toInstant(),
				TimeConfig.ZONA_PADRAO
		);

		dashboardService = new DashboardService(
				agendamentoRepository,
				authorizationService,
				clockFixo
		);

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

	private Agendamento criarAgendamento(Long id, LocalDateTime horario, StatusAgendamento status)
	{
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

	private void configurarResumo()
	{
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
		when(projection.getFaturamentoPrevisto()).thenReturn(new BigDecimal("400.00"));
	}

	@Test
	void deveRetornarIndicadoresDoDashboard() {
		configurarResumo();

		LocalDate data = hojeFixo.plusDays(1);

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

		LocalDate data = hojeFixo.plusDays(1);
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
		// Para data futura, a consulta deve começar no início do dia (00:00)
		verify(agendamentoRepository).buscarProximosAtendimentos(
				eq(estabelecimentoId),
				eq(data.atStartOfDay()),
				eq(data.plusDays(1).atStartOfDay()),
				anyList()
		);
	}

	@Test
	void deveMapearCorretamenteOsDadosDosProximosAtendimentos() {
		configurarResumo();

		LocalDate data = hojeFixo.plusDays(1);
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
	void deveUtilizarDataEHoraDoClockQuandoDataNaoForInformada() {
		configurarResumo();

		// Atendimento dentro das próximas 3 horas (15:30 local, antes das 17:00 UTC)
		var atendimentoProximo = criarAgendamento(
				99L,
				hojeFixo.atTime(15, 30),
				StatusAgendamento.CONFIRMADO
		);

		when(agendamentoRepository.buscarProximosAtendimentos(
				eq(estabelecimentoId),
				eq(agoraFixo),
				eq(hojeFixo.plusDays(1).atStartOfDay()),
				anyList()
		)).thenReturn(List.of(atendimentoProximo));

		DashboardResumoDTO resultado =
				dashboardService.consultarDashboard(
						estabelecimentoId,
						null,
						emailGestor
				);

		assertEquals(hojeFixo, resultado.data());
		assertEquals(1, resultado.proximosAtendimentos().size());
		assertEquals(99L, resultado.proximosAtendimentos().get(0).agendamentoId());

		verify(agendamentoRepository).buscarResumoDashboard(
				eq(estabelecimentoId),
				eq(hojeFixo.atStartOfDay()),
				eq(hojeFixo.plusDays(1).atStartOfDay()),
				any(),
				any(),
				any(),
				any()
		);
		verify(agendamentoRepository).buscarProximosAtendimentos(
				eq(estabelecimentoId),
				eq(agoraFixo),
				eq(hojeFixo.plusDays(1).atStartOfDay()),
				anyList()
		);
	}

	@Test
	void deveUsarDataLocalCorretaMesmoApos21hEmSaoPaulo() {
		configurarResumo();

		// 22:30 de 25/11 em São Paulo já é 01:30 de 26/11 em UTC
		LocalDateTime noiteLocal = LocalDateTime.of(2026, 11, 25, 22, 30);
		Clock clockNoite = Clock.fixed(
				noiteLocal.atZone(TimeConfig.ZONA_PADRAO).toInstant(),
				TimeConfig.ZONA_PADRAO
		);
		var serviceNoite = new DashboardService(
				agendamentoRepository,
				authorizationService,
				clockNoite
		);

		when(agendamentoRepository.buscarProximosAtendimentos(
				anyLong(), any(), any(), anyList()
		)).thenReturn(List.of());

		var resultado = serviceNoite.consultarDashboard(estabelecimentoId, null, emailGestor);

		assertEquals(LocalDate.of(2026, 11, 25), resultado.data());
		verify(agendamentoRepository).buscarProximosAtendimentos(
				eq(estabelecimentoId),
				eq(noiteLocal),
				eq(LocalDate.of(2026, 11, 26).atStartOfDay()),
				anyList()
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
				hojeFixo,
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
						hojeFixo.plusDays(1),
						emailGestor
				);

		assertNotNull(resultado.proximosAtendimentos());
		assertTrue(resultado.proximosAtendimentos().isEmpty());
	}
}
