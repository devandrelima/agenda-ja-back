package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.AgendamentoReagendamentoRequestDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.AgendamentoClienteService;
import com.agendaja.backend.service.AgendamentoReagendamentoService;
import com.agendaja.backend.service.CalculadoraDisponibilidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AgendamentoReagendamentoIntegracaoTest {
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

	@Autowired
	private AgendamentoReagendamentoService reagendamentoService;

	@Autowired
	private AgendamentoClienteService clienteService;

	@MockitoBean
	private CalculadoraDisponibilidadeService disponibilidadeService;

	private Usuario cliente;
	private Estabelecimento estabelecimento;
	private Servico servico;
	private Profissional profissional;
	private Agendamento reservaOriginal;

	@BeforeEach
	void prepararDados() {
		estabelecimento = new Estabelecimento(
				null,
				"Barbearia Integração",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		estabelecimento = estabelecimentoRepository
				.saveAndFlush(estabelecimento);

		cliente = new Usuario(
				null,
				null,
				"Cliente Teste",
				"cliente-" + UUID.randomUUID() + "@teste.com",
				"hash-de-teste",
				"CLIENTE"
		);

		cliente = usuarioRepository.saveAndFlush(cliente);

		servico = new Servico();
		servico.setNome("Corte");
		servico.setDuracaoMinutos(30);
		servico.setValor(new BigDecimal("35.00"));
		servico.setEstabelecimento(estabelecimento);
		servico.setAtivo(true);

		servico = servicoRepository.saveAndFlush(servico);

		profissional = new Profissional();
		profissional.setNome("Profissional Teste");
		profissional.setEstabelecimento(estabelecimento);
		profissional.setAtivo(true);
		profissional.setServicos(List.of(servico));

		profissional = profissionalRepository.saveAndFlush(profissional);

		LocalDateTime inicio = LocalDate.now()
				.plusDays(15)
				.atTime(10, 0);

		reservaOriginal = new Agendamento();
		reservaOriginal.setCliente(cliente);
		reservaOriginal.setServico(servico);
		reservaOriginal.setProfissional(profissional);
		reservaOriginal.setDataHoraInicio(inicio);
		reservaOriginal.setDataHoraFim(inicio.plusMinutes(30));
		reservaOriginal.setStatus(StatusAgendamento.CONFIRMADO);

		reservaOriginal = agendamentoRepository
				.saveAndFlush(reservaOriginal);
	}

	@Test
	void devePreservarReservaOriginalQuandoReagendamentoFalhar() {
		Long reservaOriginalId = reservaOriginal.getId();
		long quantidadeAntes = agendamentoRepository.count();

		LocalDateTime novoInicio = LocalDate.now()
				.plusDays(16)
				.atTime(14, 0);

		var dto = new AgendamentoReagendamentoRequestDTO(
				estabelecimento.getId(),
				servico.getId(),
				profissional.getId(),
				novoInicio
		);

		// A disponibilidade falha após o cancelamento intermediário.
		when(disponibilidadeService.calcularParaUmProfissional(
				estabelecimento.getId(),
				profissional.getId(),
				servico.getId(),
				novoInicio.toLocalDate()
		)).thenReturn(List.of());

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> reagendamentoService.reagendar(
						reservaOriginalId,
						dto,
						cliente.getEmail()
				)
		);

		assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());

		// Consulta novamente o PostgreSQL após a transação falhar.
		var reservaPersistida = agendamentoRepository
				.findById(reservaOriginalId)
				.orElseThrow();

		assertEquals(
				StatusAgendamento.CONFIRMADO,
				reservaPersistida.getStatus()
		);

		assertEquals(
				quantidadeAntes,
				agendamentoRepository.count()
		);

		verify(disponibilidadeService)
				.calcularParaUmProfissional(
						estabelecimento.getId(),
						profissional.getId(),
						servico.getId(),
						novoInicio.toLocalDate()
				);
	}

	@Test
	void deveLiberarHorarioAposCancelamento() {
		Long reservaOriginalId = reservaOriginal.getId();

		LocalDateTime inicio = reservaOriginal.getDataHoraInicio();

		// Antes de cancelar, a reserva ocupa o horário.
		var ocupadosAntes = agendamentoRepository
				.buscarAgendamentosAtivosDoDia(
						profissional.getId(),
						inicio.toLocalDate().atStartOfDay(),
						inicio.toLocalDate().plusDays(1).atStartOfDay(),
						StatusAgendamento.CANCELADO
				);

		assertTrue(ocupadosAntes.stream()
				.anyMatch(a -> a.getId().equals(reservaOriginalId)));

		// Cancelamento real, usando o service da HU19.
		var resposta = clienteService.cancelar(
				reservaOriginalId,
				cliente.getEmail()
		);

		assertEquals("CANCELADO", resposta.status());

		// A consulta de ocupações não deve mais incluir a reserva.
		var ocupadosDepois = agendamentoRepository
				.buscarAgendamentosAtivosDoDia(
						profissional.getId(),
						inicio.toLocalDate().atStartOfDay(),
						inicio.toLocalDate().plusDays(1).atStartOfDay(),
						StatusAgendamento.CANCELADO
				);

		assertTrue(ocupadosDepois.stream()
				.noneMatch(a -> a.getId().equals(reservaOriginalId)));

		assertEquals(
				StatusAgendamento.CANCELADO,
				agendamentoRepository.findById(reservaOriginalId)
						.orElseThrow()
						.getStatus()
		);
	}
}
