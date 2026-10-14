
package com.agendaja.backend.service;

import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

	private static final Long ESTABELECIMENTO_ID = 1L;
	private static final String EMAIL = "gestor@email.com";

	@Mock
	private ServicoRepository servicoRepository;

	@Mock
	private EstabelecimentoRepository estabelecimentoRepository;

	@Mock
	private EstabelecimentoAuthorizationService authorizationService;

	@InjectMocks
	private ServicoService servicoService;

	private ServicoRequestDTO dadosValidos() {
		return new ServicoRequestDTO(
				"Corte de cabelo",
				30,
				new BigDecimal("35.00")
		);
	}


	private Servico servicoExistente() {
		var estabelecimento = new Estabelecimento();
		estabelecimento.setId(ESTABELECIMENTO_ID);

		var servico = new Servico();
		servico.setId(10L);
		servico.setNome("Corte de cabelo");
		servico.setDuracaoMinutos(30);
		servico.setValor(new BigDecimal("35.00"));
		servico.setEstabelecimento(estabelecimento);

		return servico;
	}

	@Test
	void deveCriarServicoNoEstabelecimento() {
		var estabelecimento = mock(Estabelecimento.class);
		when(estabelecimento.getId()).thenReturn(ESTABELECIMENTO_ID);

		when(estabelecimentoRepository.findById(ESTABELECIMENTO_ID))
				.thenReturn(Optional.of(estabelecimento));

		when(servicoRepository.save(any(Servico.class)))
				.thenAnswer(invocation -> {
					Servico servico = invocation.getArgument(0);
					servico.setId(10L);
					return servico;
				});

		var resposta = servicoService.criar(
				ESTABELECIMENTO_ID, dadosValidos(), EMAIL
		);

		assertEquals(10L, resposta.id());
		assertEquals("Corte de cabelo", resposta.nome());
		assertEquals(30, resposta.duracaoMinutos());
		assertEquals(0,
				resposta.valor().compareTo(new BigDecimal("35.00")));
		assertEquals(ESTABELECIMENTO_ID, resposta.estabelecimentoId());

		verify(authorizationService)
				.validarAcesso(EMAIL, ESTABELECIMENTO_ID);
		verify(servicoRepository).save(any(Servico.class));
	}


	@Test
	void deveListarServicosDoEstabelecimento() {

		var servico = servicoExistente();

		when(servicoRepository.findByEstabelecimentoIdAndAtivoTrue(ESTABELECIMENTO_ID))
				.thenReturn(List.of(servico));

		var resposta = servicoService.listar(
				ESTABELECIMENTO_ID, EMAIL
		);

		assertEquals(1, resposta.size());
		assertEquals(10L, resposta.get(0).id());

		verify(servicoRepository)
				.findByEstabelecimentoIdAndAtivoTrue(ESTABELECIMENTO_ID);
	}

	@Test
	void deveConsultarServicoDoEstabelecimento() {

		var servico = servicoExistente();

		when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(
				10L, ESTABELECIMENTO_ID))
				.thenReturn(Optional.of(servico));

		var resposta = servicoService.consultar(
				ESTABELECIMENTO_ID, 10L, EMAIL
		);

		assertEquals("Corte de cabelo", resposta.nome());
		assertEquals(10L, resposta.id());
	}


	@Test
	void deveEditarServicoExistente() {
		var servico = servicoExistente();

		when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(
				10L, ESTABELECIMENTO_ID))
				.thenReturn(Optional.of(servico));

		var novosDados = new ServicoRequestDTO(
				"Corte e barba",
				45,
				new BigDecimal("50.00")
		);

		var resposta = servicoService.editar(
				ESTABELECIMENTO_ID, 10L, novosDados, EMAIL
		);

		assertEquals("Corte e barba", resposta.nome());
		assertEquals(45, resposta.duracaoMinutos());
		assertEquals(0,
				resposta.valor().compareTo(new BigDecimal("50.00")));

		assertEquals("Corte e barba", servico.getNome());

		// A persistência da edição é feita pelo JPA
		// dentro da transação, sem save() explícito.
		verify(servicoRepository, never()).save(any());
	}

	@Test
	void deveRetornar404ParaServicoDeOutroEstabelecimento() {
		when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(
				10L, ESTABELECIMENTO_ID))
				.thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> servicoService.consultar(
						ESTABELECIMENTO_ID, 10L, EMAIL
				)
		);

		assertEquals(404, excecao.getStatusCode().value());
	}

	@Test
	void deveNegarAcessoDeGestorNaoAutorizado() {
		doThrow(new AccessDeniedException("Acesso negado"))
				.when(authorizationService)
				.validarAcesso(EMAIL, ESTABELECIMENTO_ID);

		assertThrows(
				AccessDeniedException.class,
				() -> servicoService.listar(
						ESTABELECIMENTO_ID, EMAIL
				)
		);

		verifyNoInteractions(servicoRepository);
	}

	@Test
	void deveDesativarServico() {
		var servico = servicoExistente();

		when(servicoRepository
				.findByIdAndEstabelecimentoIdAndAtivoTrue(
						10L, ESTABELECIMENTO_ID
				))
				.thenReturn(Optional.of(servico));

		servicoService.desativar(
				ESTABELECIMENTO_ID,
				10L,
				EMAIL
		);

		assertFalse(servico.isAtivo());

		verify(authorizationService)
				.validarAcesso(EMAIL, ESTABELECIMENTO_ID);

		verify(servicoRepository, never()).delete(any());
	}

}
