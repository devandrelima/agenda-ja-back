
package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.DashboardService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class DashboardIntegracaoTest {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private AgendamentoRepository agendamentoRepository;

	@Autowired
	private EstabelecimentoRepository estabelecimentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ServicoRepository servicoRepository;

	@Autowired
	private ProfissionalRepository profissionalRepository;

	private Long estabelecimentoId;
	private Long outroEstabelecimentoId;

	private final String emailGestor = "gestor.dashboard@teste.com";

	private final LocalDate dataTeste = LocalDate.now().plusDays(2);

	@BeforeEach
	void setup() {
		agendamentoRepository.deleteAll();

		var estabelecimento = estabelecimentoRepository.saveAndFlush(
				new Estabelecimento(
						null,
						"Salão Dashboard",
						"SALAO",
						null,
						"Rua A"
				)
		);

		estabelecimentoId = estabelecimento.getId();

		usuarioRepository.saveAndFlush(
				new Usuario(
						null,
						estabelecimento,
						"Gestor Dashboard",
						emailGestor,
						"hash",
						"GESTOR"
				)
		);

		var cliente = usuarioRepository.saveAndFlush(
				new Usuario(
						null,
						null,
						"Cliente Dashboard",
						"cliente.dashboard@teste.com",
						"hash",
						"CLIENTE"
				)
		);

		var servico = criarServico(estabelecimento, "Corte", "50.00");
		var profissional = criarProfissional(estabelecimento, servico, "João");

		LocalDateTime horarioBase = dataTeste.atTime(8, 0);

		// Dois agendamentos confirmados.
		criarAgendamento(
				cliente,
				profissional,
				servico,
				horarioBase,
				StatusAgendamento.CONFIRMADO
		);

		criarAgendamento(
				cliente,
				profissional,
				servico,
				horarioBase.plusHours(1),
				StatusAgendamento.CONFIRMADO
		);

		// Cinco agendamentos pendentes.
		for (int i = 2; i < 7; i++) {
			criarAgendamento(
					cliente,
					profissional,
					servico,
					horarioBase.plusHours(i),
					StatusAgendamento.AGUARDANDO_CONFIRMACAO
			);
		}

		// Um cancelado.
		criarAgendamento(
				cliente,
				profissional,
				servico,
				horarioBase.plusHours(7),
				StatusAgendamento.CANCELADO
		);

		// Um concluído.
		criarAgendamento(
				cliente,
				profissional,
				servico,
				horarioBase.plusHours(8),
				StatusAgendamento.CONCLUIDO
		);

		// Outro estabelecimento, que não deve entrar nos indicadores.
		var outroEstabelecimento = estabelecimentoRepository.saveAndFlush(
				new Estabelecimento(
						null,
						"Outro Salão",
						"SALAO",
						null,
						"Rua B"
				)
		);

		outroEstabelecimentoId = outroEstabelecimento.getId();

		var outroServico = criarServico(
				outroEstabelecimento,
				"Barba",
				"100.00"
		);

		var outroProfissional = criarProfissional(
				outroEstabelecimento,
				outroServico,
				"Pedro"
		);

		criarAgendamento(
				cliente,
				outroProfissional,
				outroServico,
				horarioBase,
				StatusAgendamento.CONFIRMADO
		);
	}

	private Servico criarServico(
			Estabelecimento estabelecimento,
			String nome,
			String valor
	) {
		var servico = new Servico();
		servico.setNome(nome);
		servico.setDuracaoMinutos(30);
		servico.setValor(new BigDecimal(valor));
		servico.setAtivo(true);
		servico.setEstabelecimento(estabelecimento);

		return servicoRepository.saveAndFlush(servico);
	}

	private Profissional criarProfissional(
			Estabelecimento estabelecimento,
			Servico servico,
			String nome
	) {
		var profissional = new Profissional();
		profissional.setNome(nome);
		profissional.setEstabelecimento(estabelecimento);
		profissional.setServicos(List.of(servico));

		return profissionalRepository.saveAndFlush(profissional);
	}

	private Agendamento criarAgendamento(
			Usuario cliente,
			Profissional profissional,
			Servico servico,
			LocalDateTime horario,
			StatusAgendamento status
	) {
		var agendamento = new Agendamento();
		agendamento.setCliente(cliente);
		agendamento.setProfissional(profissional);
		agendamento.setServico(servico);
		agendamento.setDataHoraInicio(horario);
		agendamento.setDataHoraFim(horario.plusMinutes(30));
		agendamento.setStatus(status);

		return agendamentoRepository.saveAndFlush(agendamento);
	}

	@Test
	void deveRetornarQuantidadeCorretaDeAgendamentosPorStatus() {
		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				dataTeste,
				emailGestor
		);

		assertEquals(9L, resultado.totalReservas());
		assertEquals(2L, resultado.confirmados());
		assertEquals(5L, resultado.pendentes());
		assertEquals(1L, resultado.cancelados());
		assertEquals(1L, resultado.concluidos());
	}

	@Test
	void deveCalcularFaturamentoPrevistoExcluindoCancelados() {
		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				dataTeste,
				emailGestor
		);

		// Oito agendamentos não cancelados x R$ 50,00.
		assertEquals(
				0,
				new BigDecimal("400.00").compareTo(
						resultado.faturamentoPrevisto()
				)
		);
	}

	@Test
	void deveRetornarTodosOsAtendimentosFuturosPendentesOuConfirmados() {
		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				dataTeste,
				emailGestor
		);

		// Dois confirmados + cinco pendentes.
		assertEquals(7, resultado.proximosAtendimentos().size());

		assertTrue(
				resultado.proximosAtendimentos().stream()
						.allMatch(atendimento ->
								atendimento.status().equals("CONFIRMADO")
										|| atendimento.status().equals("AGUARDANDO_CONFIRMACAO")
						)
		);
	}

	@Test
	void deveOrdenarProximosAtendimentosPorHorario() {
		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				dataTeste,
				emailGestor
		);

		var atendimentos = resultado.proximosAtendimentos();

		for (int i = 1; i < atendimentos.size(); i++) {
			assertFalse(
					atendimentos.get(i).horarioInicio()
							.isBefore(atendimentos.get(i - 1).horarioInicio())
			);
		}
	}

	@Test
	void naoDeveIncluirAgendamentosDeOutroEstabelecimento() {
		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				dataTeste,
				emailGestor
		);

		assertEquals(9L, resultado.totalReservas());
		assertEquals(
				0,
				new BigDecimal("400.00").compareTo(
						resultado.faturamentoPrevisto()
				)
		);
	}

	@Test
	void deveNegarAcessoAoDashboardDeOutroEstabelecimento() {
		assertThrows(
				AccessDeniedException.class,
				() -> dashboardService.consultarDashboard(
						outroEstabelecimentoId,
						dataTeste,
						emailGestor
				)
		);
	}

	@Test
	void deveRetornarIndicadoresZeradosQuandoNaoExistiremAgendamentos() {
		LocalDate diaSemAgendamentos = dataTeste.plusDays(1);

		var resultado = dashboardService.consultarDashboard(
				estabelecimentoId,
				diaSemAgendamentos,
				emailGestor
		);

		assertEquals(0L, resultado.totalReservas());
		assertEquals(0L, resultado.confirmados());
		assertEquals(0L, resultado.pendentes());
		assertEquals(0L, resultado.cancelados());
		assertEquals(0L, resultado.concluidos());

		assertEquals(
				0,
				BigDecimal.ZERO.compareTo(resultado.faturamentoPrevisto())
		);

		assertTrue(resultado.proximosAtendimentos().isEmpty());
	}
}
