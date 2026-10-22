package com.agendaja.backend.service;

import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ServicoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoPublicoServiceTest {

	private static final Long ESTABELECIMENTO_ID = 1L;

	@Mock
	private ServicoRepository servicoRepository;

	@Mock
	private EstabelecimentoRepository estabelecimentoRepository;

	@InjectMocks
	private ServicoPublicoService servicoPublicoService;

	private Servico criarServico() {
		var estabelecimento = new Estabelecimento();
		estabelecimento.setId(ESTABELECIMENTO_ID);

		var servico = new Servico();
		servico.setId(10L);
		servico.setNome("Corte de cabelo");
		servico.setDuracaoMinutos(30);
		servico.setValor(new BigDecimal("35.00"));
		servico.setEstabelecimento(estabelecimento);
		servico.setAtivo(true);

		return servico;
	}

	@Test
	void deveListarServicosAtivosDoEstabelecimento() {

		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		var servico = criarServico();

		when(servicoRepository
				.findByEstabelecimentoIdAndAtivoTrue(ESTABELECIMENTO_ID))
				.thenReturn(List.of(servico));

		var resposta = servicoPublicoService.listar(ESTABELECIMENTO_ID);

		assertEquals(1, resposta.size());
		assertEquals(10L, resposta.get(0).id());
		assertEquals("Corte de cabelo", resposta.get(0).nome());
		assertEquals(30, resposta.get(0).duracaoMinutos());
		assertEquals(0, resposta.get(0).valor()
				.compareTo(new BigDecimal("35.00")));

		verify(servicoRepository)
				.findByEstabelecimentoIdAndAtivoTrue(ESTABELECIMENTO_ID);
	}

	@Test
	void deveRetornarListaVaziaQuandoNaoHaServicosAtivos() {

		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		var resposta = servicoPublicoService.listar(ESTABELECIMENTO_ID);

		assertTrue(resposta.isEmpty());
	}

	@Test
	void deveRetornar404QuandoEstabelecimentoNaoExiste() {

		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(false);

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> servicoPublicoService.listar(ESTABELECIMENTO_ID)
		);

		assertEquals(404, excecao.getStatusCode().value());

		verifyNoInteractions(servicoRepository);
	}

	@Test
	void deveConsultarServicoAtivoDoEstabelecimento() {

		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		var servico = criarServico();

		when(servicoRepository
				.findByIdAndEstabelecimentoIdAndAtivoTrue(
						10L, ESTABELECIMENTO_ID
				))
				.thenReturn(Optional.of(servico));

		var resposta = servicoPublicoService.consultar(
				ESTABELECIMENTO_ID, 10L
		);

		assertEquals(10L, resposta.id());
		assertEquals("Corte de cabelo", resposta.nome());
		assertEquals(ESTABELECIMENTO_ID, resposta.estabelecimentoId());
	}

	@Test
	void deveRetornar404QuandoServicoNaoPertenceAoEstabelecimento() {

		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		when(servicoRepository
				.findByIdAndEstabelecimentoIdAndAtivoTrue(
						10L, ESTABELECIMENTO_ID
				))
				.thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> servicoPublicoService.consultar(
						ESTABELECIMENTO_ID, 10L
				)
		);

		assertEquals(404, excecao.getStatusCode().value());
	}
}
