package com.agendaja.backend.service;

import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoPublicoServiceTest {
	@Mock
	private EstabelecimentoRepository repository;

	@Mock
	private ConfiguracaoEstabelecimentoService configuracaoService;

	@Mock
    private AvaliacaoRepository avaliacaoRepository;

	@InjectMocks
	private EstabelecimentoPublicoService service;

	@Test
	void deveRetornarDadosPublicosDoEstabelecimento() {
		UUID publicId = UUID.randomUUID();

		var estabelecimento = new Estabelecimento(
				1L,
				"Barbearia Central",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		estabelecimento.setPublicId(publicId);

		when(repository.findByPublicId(publicId))
				.thenReturn(Optional.of(estabelecimento));

		var resposta = service.consultar(publicId);

		assertEquals(1L, resposta.id());
		assertEquals(publicId, resposta.publicId());
		assertEquals("Barbearia Central", resposta.nome());
		assertEquals("BARBEARIA", resposta.categoria());
		assertEquals("Mossoró/RN", resposta.localizacao());

		verify(repository).findByPublicId(publicId);
	}

	@Test
	void deveRetornar404QuandoEstabelecimentoNaoExiste() {
		UUID publicId = UUID.randomUUID();

		when(repository.findByPublicId(publicId))
				.thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> service.consultar(publicId)
		);

		assertEquals(404, excecao.getStatusCode().value());
	}

	@Test
	void deveManterIdentificadorNumericoNaResposta() {
		UUID publicId = UUID.randomUUID();

		var estabelecimento = new Estabelecimento(
				25L,
				"Salão Teste",
				"SALAO",
				null,
				"Natal/RN"
		);

		estabelecimento.setPublicId(publicId);

		when(repository.findByPublicId(publicId))
				.thenReturn(Optional.of(estabelecimento));

		var resposta = service.consultar(publicId);

		assertEquals(25L, resposta.id());
		assertEquals(publicId, resposta.publicId());
	}

	@Test
    void deveListarResumoPublicoDosEstabelecimentos() {
        var estabelecimento = new Estabelecimento(
                1L,
                "Barbearia Central",
                "BARBEARIA",
                null,
                "Mossoró/RN"
        );
        estabelecimento.setPublicId(UUID.randomUUID());

        when(repository.findAll()).thenReturn(java.util.List.of(estabelecimento));
        when(avaliacaoRepository.calcularMediaPorEstabelecimento(1L)).thenReturn(4.8);
        when(avaliacaoRepository.countByEstabelecimentoId(1L)).thenReturn(15L);

        var lista = service.listar();

        assertEquals(1, lista.size());
        assertEquals("Barbearia Central", lista.get(0).nome());
        assertEquals(15L, ((Number) lista.get(0).quantidadeAvaliacoes()).longValue());
    }
}
