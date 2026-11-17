package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.RelatorioOperacionalDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.RelatorioOperacionalService;

import jakarta.persistence.EntityManager;

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
class RelatorioOperacionalIntegracaoTest {

	@Autowired
	private RelatorioOperacionalService relatorioService;

	@Autowired
	private EstabelecimentoRepository estabelecimentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ServicoRepository servicoRepository;

	@Autowired
	private ProfissionalRepository profissionalRepository;

	@Autowired
	private AgendamentoRepository agendamentoRepository;

	@Autowired
	private EntityManager entityManager;

	private Long estabelecimentoId;
	private Long outroEstabelecimentoId;

	private Usuario cliente;
	private Servico corte;
	private Servico barba;
	private Profissional profissionalCorte;
	private Profissional profissionalBarba;

	private final String emailGestor = "gestor.relatorio@teste.com";

	private final LocalDate dataInicio =
			LocalDate.of(2026, 10, 10);

	private final LocalDate dataFim =
			LocalDate.of(2026, 10, 20);

	@BeforeEach
	void setup() {
		var estabelecimento = estabelecimentoRepository.saveAndFlush(
				new Estabelecimento(
						null,
						"Salão Relatório",
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
						"Gestor Relatório",
						emailGestor,
						"hash",
						"GESTOR"
				)
		);

		cliente = usuarioRepository.saveAndFlush(
				new Usuario(
						null,
						null,
						"Cliente Relatório",
						"cliente.relatorio@teste.com",
						"hash",
						"CLIENTE"
				)
		);

		corte = criarServico(
				estabelecimento,
				"Corte",
				"50.00"
		);

		barba = criarServico(
				estabelecimento,
				"Barba",
				"80.00"
		);

		profissionalCorte = criarProfissional(
				estabelecimento,
				corte,
				"João"
		);

		profissionalBarba = criarProfissional(
				estabelecimento,
				barba,
				"Pedro"
		);

		criarDadosRelatorio();

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
				"Massagem",
				"200.00"
		);

		var outroProfissional = criarProfissional(
				outroEstabelecimento,
				outroServico,
				"Carlos"
		);

		// Agendamento de outro estabelecimento.
		criarAgendamento(
				outroProfissional,
				outroServico,
				dataInicio.atTime(10, 0),
				StatusAgendamento.CONCLUIDO
		);

		// Garante que as consultas leiam os dados persistidos.
		entityManager.flush();
		entityManager.clear();
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

	private void criarDadosRelatorio() {
		// Primeiro dia do período: concluído.
		criarAgendamento(
				profissionalCorte,
				corte,
				dataInicio.atTime(8, 0),
				StatusAgendamento.CONCLUIDO
		);

		// Meio do período: outro concluído.
		criarAgendamento(
				profissionalCorte,
				corte,
				dataInicio.plusDays(3).atTime(8, 0),
				StatusAgendamento.CONCLUIDO
		);

		// Meio do período: confirmado.
		criarAgendamento(
				profissionalCorte,
				corte,
				dataInicio.plusDays(3).atTime(9, 0),
				StatusAgendamento.CONFIRMADO
		);

		// Meio do período: cancelado.
		criarAgendamento(
				profissionalCorte,
				corte,
				dataInicio.plusDays(3).atTime(10, 0),
				StatusAgendamento.CANCELADO
		);

		// Último dia do período: concluído.
		criarAgendamento(
				profissionalBarba,
				barba,
				dataFim.atTime(8, 0),
				StatusAgendamento.CONCLUIDO
		);

		// Meio do período: pendente.
		criarAgendamento(
				profissionalBarba,
				barba,
				dataInicio.plusDays(3).atTime(11, 0),
				StatusAgendamento.AGUARDANDO_CONFIRMACAO
		);

		// Antes do período: não deve entrar no relatório.
		criarAgendamento(
				profissionalCorte,
				corte,
				dataInicio.minusDays(1).atTime(8, 0),
				StatusAgendamento.CONCLUIDO
		);

		// Depois do período: também não deve entrar.
		criarAgendamento(
				profissionalBarba,
				barba,
				dataFim.plusDays(1).atTime(8, 0),
				StatusAgendamento.CONCLUIDO
		);
	}

	@Test
	void deveCalcularCorretamenteOsIndicadoresDoRelatorio() {
		RelatorioOperacionalDTO resultado =
				relatorioService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		assertEquals(3L, resultado.totalConcluidos());

		assertEquals(
				0,
				new BigDecimal("180.00").compareTo(
						resultado.faturamentoConcluidos()
				)
		);

		// (3 concluídos + 1 confirmado) / 5 não cancelados = 80%.
		assertEquals(
				0,
				new BigDecimal("80.00").compareTo(
						resultado.taxaConfirmacao()
				)
		);
	}

	@Test
	void deveRetornarServicosMaisPopularesEmOrdemDeFrequencia() {
		RelatorioOperacionalDTO resultado =
				relatorioService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		var servicos = resultado.servicosMaisPopulares();

		assertEquals(2, servicos.size());

		assertEquals("Corte", servicos.get(0).servicoNome());
		assertEquals(
				3L,
				servicos.get(0).quantidadeAgendamentos()
		);

		assertEquals("Barba", servicos.get(1).servicoNome());
		assertEquals(
				2L,
				servicos.get(1).quantidadeAgendamentos()
		);
	}

	@Test
	void deveIncluirAsDatasInicialEFinalEExcluirDatasForaDoPeriodo() {
		RelatorioOperacionalDTO resultado =
				relatorioService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		// Os três concluídos incluem o primeiro e o último dia.
		assertEquals(3L, resultado.totalConcluidos());

		// Os concluídos anteriores e posteriores ao período
		// não devem ser contabilizados.
		assertEquals(
				0,
				new BigDecimal("180.00").compareTo(
						resultado.faturamentoConcluidos()
				)
		);
	}

	@Test
	void naoDeveIncluirDadosDeOutroEstabelecimento() {
		RelatorioOperacionalDTO resultado =
				relatorioService.consultarRelatorio(
						estabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				);

		// O outro estabelecimento possui um concluído de R$ 200,00.
		// Ele não pode alterar os indicadores deste relatório.
		assertEquals(3L, resultado.totalConcluidos());

		assertEquals(
				0,
				new BigDecimal("180.00").compareTo(
						resultado.faturamentoConcluidos()
				)
		);

		assertTrue(
				resultado.servicosMaisPopulares().stream()
						.noneMatch(servico ->
								servico.servicoNome().equals("Massagem")
						)
		);
	}

	@Test
	void deveRetornarValoresZeradosQuandoNaoExistiremAgendamentosNoPeriodo() {
		LocalDate inicioSemDados =
				LocalDate.of(2026, 12, 1);

		LocalDate fimSemDados =
				LocalDate.of(2026, 12, 31);

		RelatorioOperacionalDTO resultado =
				relatorioService.consultarRelatorio(
						estabelecimentoId,
						inicioSemDados,
						fimSemDados,
						emailGestor
				);

		assertEquals(0L, resultado.totalConcluidos());

		assertEquals(
				0,
				BigDecimal.ZERO.compareTo(
						resultado.faturamentoConcluidos()
				)
		);

		assertEquals(
				0,
				new BigDecimal("0.00").compareTo(
						resultado.taxaConfirmacao()
				)
		);

		assertTrue(resultado.servicosMaisPopulares().isEmpty());
	}

	@Test
	void deveNegarAcessoAoRelatorioDeOutroEstabelecimento() {
		assertThrows(
				AccessDeniedException.class,
				() -> relatorioService.consultarRelatorio(
						outroEstabelecimentoId,
						dataInicio,
						dataFim,
						emailGestor
				)
		);
	}
}
