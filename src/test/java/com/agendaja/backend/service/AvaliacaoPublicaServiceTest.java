package com.agendaja.backend.service;

import com.agendaja.backend.model.Avaliacao;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoPublicaServiceTest {

	private static final Long ESTABELECIMENTO_ID = 1L;

	@Mock
	private AvaliacaoRepository avaliacaoRepository;

	@Mock
	private EstabelecimentoRepository estabelecimentoRepository;

	@InjectMocks
	private AvaliacaoPublicaService service;

	private Avaliacao criarAvaliacao(
			Long id,
			String nomeCliente,
			int nota
	) {
		var cliente = new Usuario();
		cliente.setId(id);
		cliente.setNome(nomeCliente);

		var avaliacao = new Avaliacao();
		avaliacao.setId(id);
		avaliacao.setCliente(cliente);
		avaliacao.setNota(nota);
		avaliacao.setComentario("Comentário de teste");

		return avaliacao;
	}

	@Test
	void deveListarAvaliacoesDoEstabelecimento() {
		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		when(avaliacaoRepository
				.findByEstabelecimentoIdOrderByCriadoEmDesc(
						ESTABELECIMENTO_ID
				))
				.thenReturn(List.of(
						criarAvaliacao(10L, "Maria", 5),
						criarAvaliacao(11L, "João", 4)
				));

		var resposta = service.listar(ESTABELECIMENTO_ID);

		assertEquals(2, resposta.size());
		assertEquals("Maria", resposta.get(0).nomeCliente());
		assertEquals(5, resposta.get(0).nota());
		assertEquals("João", resposta.get(1).nomeCliente());

		verify(avaliacaoRepository)
				.findByEstabelecimentoIdOrderByCriadoEmDesc(
						ESTABELECIMENTO_ID
				);
	}

	@Test
	void deveRetornarListaVaziaSemAvaliacoes() {
		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		var resposta = service.listar(ESTABELECIMENTO_ID);

		assertTrue(resposta.isEmpty());
	}

	@Test
	void deveCalcularResumoComAvaliacoes() {
		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		when(avaliacaoRepository.countByEstabelecimentoId(
				ESTABELECIMENTO_ID
		)).thenReturn(3L);

		when(avaliacaoRepository.calcularMediaPorEstabelecimento(
				ESTABELECIMENTO_ID
		)).thenReturn(4.333333333333333);

		var resumo = service.consultarResumo(ESTABELECIMENTO_ID);

		assertEquals(3L, resumo.totalAvaliacoes());
		assertEquals(0, resumo.mediaNotas()
				.compareTo(new java.math.BigDecimal("4.33")));
	}

	@Test
	void deveRetornarMediaZeroSemAvaliacoes() {
		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(true);

		when(avaliacaoRepository.countByEstabelecimentoId(
				ESTABELECIMENTO_ID
		)).thenReturn(0L);

		var resumo = service.consultarResumo(ESTABELECIMENTO_ID);

		assertEquals(0L, resumo.totalAvaliacoes());
		assertEquals(0, resumo.mediaNotas()
				.compareTo(java.math.BigDecimal.ZERO));

		verify(avaliacaoRepository, never())
				.calcularMediaPorEstabelecimento(anyLong());
	}

	@Test
	void deveRetornar404ParaEstabelecimentoInexistente() {
		when(estabelecimentoRepository.existsById(ESTABELECIMENTO_ID))
				.thenReturn(false);

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> service.consultarResumo(ESTABELECIMENTO_ID)
		);

		assertEquals(404, excecao.getStatusCode().value());
		verifyNoInteractions(avaliacaoRepository);
	}
}
