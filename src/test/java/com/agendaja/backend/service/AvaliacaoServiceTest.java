package com.agendaja.backend.service;

import com.agendaja.backend.dto.AvaliacaoRequestDTO;
import com.agendaja.backend.model.Avaliacao;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

	private static final Long ESTABELECIMENTO_ID = 1L;
	private static final Long CLIENTE_ID = 10L;
	private static final String EMAIL = "cliente@email.com";

	@Mock
	private AvaliacaoRepository avaliacaoRepository;

	@Mock
	private EstabelecimentoRepository estabelecimentoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@InjectMocks
	private AvaliacaoService avaliacaoService;

	private Usuario criarCliente() {
		var cliente = new Usuario();
		cliente.setId(CLIENTE_ID);
		cliente.setNome("Maria Silva");
		cliente.setEmail(EMAIL);
		cliente.setRole("CLIENTE");
		return cliente;
	}

	private Estabelecimento criarEstabelecimento() {
		var estabelecimento = new Estabelecimento();
		estabelecimento.setId(ESTABELECIMENTO_ID);
		return estabelecimento;
	}

	private Avaliacao criarAvaliacao() {
		var avaliacao = new Avaliacao();
		avaliacao.setId(100L);
		avaliacao.setCliente(criarCliente());
		avaliacao.setEstabelecimento(criarEstabelecimento());
		avaliacao.setNota(5);
		avaliacao.setComentario("Excelente atendimento!");
		return avaliacao;
	}

	@Test
	void devePublicarAvaliacaoComClienteAutenticado() {
		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(estabelecimentoRepository.findById(ESTABELECIMENTO_ID))
				.thenReturn(Optional.of(criarEstabelecimento()));

		when(avaliacaoRepository.saveAndFlush(any(Avaliacao.class)))
				.thenAnswer(invocation -> {
					Avaliacao avaliacao = invocation.getArgument(0);
					avaliacao.setId(100L);
					return avaliacao;
				});

		var resposta = avaliacaoService.publicar(
				ESTABELECIMENTO_ID,
				new AvaliacaoRequestDTO(5, "Excelente atendimento!"),
				EMAIL
		);

		assertEquals(100L, resposta.id());
		assertEquals(CLIENTE_ID, resposta.clienteId());
		assertEquals(5, resposta.nota());
		assertEquals("Excelente atendimento!", resposta.comentario());

		verify(avaliacaoRepository).saveAndFlush(argThat(a ->
				a.getCliente().getId().equals(CLIENTE_ID)
						&& a.getEstabelecimento().getId().equals(ESTABELECIMENTO_ID)
						&& a.getNota() == 5
		));
	}

	@Test
	void deveRejeitarAvaliacaoDuplicada() {
		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(estabelecimentoRepository.findById(ESTABELECIMENTO_ID))
				.thenReturn(Optional.of(criarEstabelecimento()));

		when(avaliacaoRepository.existsByClienteIdAndEstabelecimentoId(
				CLIENTE_ID, ESTABELECIMENTO_ID
		)).thenReturn(true);

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> avaliacaoService.publicar(
						ESTABELECIMENTO_ID,
						new AvaliacaoRequestDTO(4, "Bom atendimento"),
						EMAIL
				)
		);

		assertEquals(409, excecao.getStatusCode().value());
		verify(avaliacaoRepository, never())
				.saveAndFlush(any(Avaliacao.class));
	}

	@Test
	void deveEditarPropriaAvaliacao() {
		var avaliacao = criarAvaliacao();

		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(avaliacaoRepository.findByClienteIdAndEstabelecimentoId(
				CLIENTE_ID, ESTABELECIMENTO_ID
		)).thenReturn(Optional.of(avaliacao));

		var resposta = avaliacaoService.editar(
				ESTABELECIMENTO_ID,
				new AvaliacaoRequestDTO(3, "Atendimento razoável"),
				EMAIL
		);

		assertEquals(3, resposta.nota());
		assertEquals("Atendimento razoável", resposta.comentario());

		verify(avaliacaoRepository).flush();
	}

	@Test
	void naoDeveEditarAvaliacaoDeOutroCliente() {
		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(avaliacaoRepository.findByClienteIdAndEstabelecimentoId(
				CLIENTE_ID, ESTABELECIMENTO_ID
		)).thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> avaliacaoService.editar(
						ESTABELECIMENTO_ID,
						new AvaliacaoRequestDTO(1, "Comentário alterado"),
						EMAIL
				)
		);

		assertEquals(404, excecao.getStatusCode().value());
		verify(avaliacaoRepository, never()).flush();
	}

	@Test
	void deveExcluirPropriaAvaliacao() {
		var avaliacao = criarAvaliacao();

		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(avaliacaoRepository.findByClienteIdAndEstabelecimentoId(
				CLIENTE_ID, ESTABELECIMENTO_ID
		)).thenReturn(Optional.of(avaliacao));

		avaliacaoService.excluir(ESTABELECIMENTO_ID, EMAIL);

		verify(avaliacaoRepository).delete(avaliacao);
	}

	@Test
	void deveImpedirGestorDePublicarAvaliacao() {
		var gestor = criarCliente();
		gestor.setRole("GESTOR");

		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(gestor));

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> avaliacaoService.publicar(
						ESTABELECIMENTO_ID,
						new AvaliacaoRequestDTO(5, "Teste"),
						EMAIL
				)
		);

		assertEquals(403, excecao.getStatusCode().value());
		verifyNoInteractions(avaliacaoRepository);
	}

	@Test
	void naoDeveExcluirAvaliacaoDeOutroCliente() {
		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(criarCliente()));

		when(avaliacaoRepository.findByClienteIdAndEstabelecimentoId(
				CLIENTE_ID, ESTABELECIMENTO_ID
		)).thenReturn(Optional.empty());

		var excecao = assertThrows(
				ResponseStatusException.class,
				() -> avaliacaoService.excluir(
						ESTABELECIMENTO_ID,
						EMAIL
				)
		);

		assertEquals(404, excecao.getStatusCode().value());
		verify(avaliacaoRepository, never())
				.delete(any(Avaliacao.class));
	}
}
