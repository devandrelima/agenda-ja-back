package com.agendaja.backend.service;

import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoClienteServiceTest {

	@Mock
	AgendamentoRepository agendamentoRepository;

	@Mock
	UsuarioRepository usuarioRepository;

	@InjectMocks
	AgendamentoClienteService service;

	Usuario cliente;
	Agendamento agendamento;

	final String email = "cliente@teste.com";

	@BeforeEach
	void configurar() {
		cliente = new Usuario(
				10L,
				null,
				"Cliente",
				email,
				"hash",
				"CLIENTE"
		);

		var estabelecimento = new Estabelecimento(
				1L,
				"Salão",
				"SALAO",
				null,
				"Centro"
		);
		estabelecimento.setPublicId(UUID.randomUUID());

		var servico = new Servico();
		servico.setId(2L);
		servico.setNome("Corte");
		servico.setValor(new BigDecimal("50.00"));

		var profissional = new Profissional();
		profissional.setId(3L);
		profissional.setNome("João");
		profissional.setEstabelecimento(estabelecimento);

		agendamento = new Agendamento();
		agendamento.setId(4L);
		agendamento.setCliente(cliente);
		agendamento.setProfissional(profissional);
		agendamento.setServico(servico);
		agendamento.setStatus(
				StatusAgendamento.AGUARDANDO_CONFIRMACAO
		);
		agendamento.setDataHoraInicio(
				LocalDateTime.of(2026, 12, 1, 10, 0)
		);
		agendamento.setDataHoraFim(
				LocalDateTime.of(2026, 12, 1, 10, 30)
		);
	}

	@Test
	void deveListarApenasAgendamentosDoCliente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository
				.findByClienteIdOrderByDataHoraInicioAsc(10L))
				.thenReturn(List.of(agendamento));

		var lista = service.listar(email);

		assertEquals(1, lista.size());
		assertEquals(4L, lista.get(0).id());
		assertEquals(1L, lista.get(0).estabelecimentoId());
		assertNotNull(
				lista.get(0).estabelecimentoPublicId()
		);

		verify(agendamentoRepository)
				.findByClienteIdOrderByDataHoraInicioAsc(10L);
	}

	@Test
	void deveRetornar404ParaReservaDeOutroCliente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository
				.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.empty());

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.consultar(4L, email)
		);

		assertEquals(404, erro.getStatusCode().value());
	}

	@Test
	void deveConfirmarReservaPendente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.confirmarSePendente(
				4L,
				10L,
				StatusAgendamento.AGUARDANDO_CONFIRMACAO,
				StatusAgendamento.CONFIRMADO
		)).thenAnswer(invocation -> {
			agendamento.setStatus(StatusAgendamento.CONFIRMADO);
			return 1;
		});

		when(agendamentoRepository.findByIdAndClienteId(
				4L,
				10L
		)).thenReturn(Optional.of(agendamento));

		var resposta = service.confirmar(4L, email);

		assertEquals("CONFIRMADO", resposta.status());
	}

	@Test
	void deveRetornar409ParaReservaNaoPendente() {
		agendamento.setStatus(StatusAgendamento.CONFIRMADO);

		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.findByIdAndClienteId(
				4L,
				10L
		)).thenReturn(Optional.of(agendamento));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.confirmar(4L, email)
		);

		assertEquals(409, erro.getStatusCode().value());
	}

	@Test
	void deveRetornar404AoConfirmarReservaDeOutroCliente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.confirmar(4L, email)
		);

		assertEquals(404, erro.getStatusCode().value());
	}

	@Test
	void deveImpedirGestorDeConsultarLista() {
		cliente.setRole("GESTOR");

		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.listar(email)
		);

		assertEquals(403, erro.getStatusCode().value());

		verifyNoInteractions(agendamentoRepository);
	}

	@Test
	void deveCancelarAgendamentoPendente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.cancelarSePermitido(
				eq(4L),
				eq(10L),
				anyList(),
				eq(StatusAgendamento.CANCELADO)
		)).thenAnswer(invocation -> {
			agendamento.setStatus(StatusAgendamento.CANCELADO);
			return 1;
		});

		when(agendamentoRepository.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.of(agendamento));

		var resposta = service.cancelar(4L, email);

		assertEquals("CANCELADO", resposta.status());

		verify(agendamentoRepository).cancelarSePermitido(
				4L,
				10L,
				List.of(
						StatusAgendamento.AGUARDANDO_CONFIRMACAO,
						StatusAgendamento.CONFIRMADO
				),
				StatusAgendamento.CANCELADO
		);
	}

	@Test
	void deveCancelarAgendamentoConfirmado() {
		agendamento.setStatus(StatusAgendamento.CONFIRMADO);

		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.cancelarSePermitido(
				eq(4L),
				eq(10L),
				anyList(),
				eq(StatusAgendamento.CANCELADO)
		)).thenAnswer(invocation -> {
			agendamento.setStatus(StatusAgendamento.CANCELADO);
			return 1;
		});

		when(agendamentoRepository.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.of(agendamento));

		var resposta = service.cancelar(4L, email);

		assertEquals("CANCELADO", resposta.status());
	}

	@Test
	void deveRejeitarCancelamentoDeAgendamentoJaCancelado() {
		agendamento.setStatus(StatusAgendamento.CANCELADO);

		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.of(agendamento));

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> service.cancelar(4L, email)
		);

		assertEquals(409, excecao.getStatusCode().value());
	}

	@Test
	void deveRejeitarCancelamentoDeAgendamentoConcluido() {
		agendamento.setStatus(StatusAgendamento.CONCLUIDO);

		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.of(agendamento));

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> service.cancelar(4L, email)
		);

		assertEquals(409, excecao.getStatusCode().value());
	}

	@Test
	void deveImpedirCancelamentoDeOutroCliente() {
		when(usuarioRepository.findByEmail(email))
				.thenReturn(Optional.of(cliente));

		when(agendamentoRepository.findByIdAndClienteId(4L, 10L))
				.thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> service.cancelar(4L, email)
		);

		assertEquals(404, excecao.getStatusCode().value());

		verify(agendamentoRepository, never())
				.delete(any(Agendamento.class));
	}
}
