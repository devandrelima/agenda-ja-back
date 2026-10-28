
package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.AvaliacaoRequestDTO;
import com.agendaja.backend.model.Avaliacao;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.service.AvaliacaoPublicaService;
import com.agendaja.backend.service.AvaliacaoService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AvaliacaoIntegracaoTest {

	@Autowired
	private AvaliacaoRepository avaliacaoRepository;

	@Autowired
	private EstabelecimentoRepository estabelecimentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AvaliacaoService avaliacaoService;

	@Autowired
	private AvaliacaoPublicaService avaliacaoPublicaService;

	@Autowired
	private EntityManager entityManager;

	private Estabelecimento criarEstabelecimento(String nome) {
		var estabelecimento = new Estabelecimento(
				null,
				nome,
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		return estabelecimentoRepository.saveAndFlush(estabelecimento);
	}

	private Usuario criarCliente(String nome, String email) {
		var cliente = new Usuario(
				null,
				null,
				nome,
				email,
				"hash-de-teste",
				"CLIENTE"
		);

		cliente.setTelefone(null);

		return usuarioRepository.saveAndFlush(cliente);
	}

	private Avaliacao criarAvaliacao(
			Usuario cliente,
			Estabelecimento estabelecimento,
			int nota
	) {
		var avaliacao = new Avaliacao();

		avaliacao.setCliente(cliente);
		avaliacao.setEstabelecimento(estabelecimento);
		avaliacao.setNota(nota);
		avaliacao.setComentario("Atendimento de teste");

		return avaliacaoRepository.saveAndFlush(avaliacao);
	}

	private void sincronizarBanco() {
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void devePersistirAvaliacaoNoPostgreSQL() {
		var estabelecimento = criarEstabelecimento(
				"Barbearia Integração"
		);

		var cliente = criarCliente(
				"Maria Silva",
				"maria.avaliacao@teste.com"
		);

		var avaliacao = criarAvaliacao(
				cliente,
				estabelecimento,
				5
		);

		Long avaliacaoId = avaliacao.getId();

		sincronizarBanco();

		var persistida = avaliacaoRepository
				.findById(avaliacaoId)
				.orElseThrow();

		assertEquals(5, persistida.getNota());
		assertEquals(
				"Atendimento de teste",
				persistida.getComentario()
		);

		assertEquals(
				cliente.getId(),
				persistida.getCliente().getId()
		);

		assertEquals(
				estabelecimento.getId(),
				persistida.getEstabelecimento().getId()
		);

		assertNotNull(persistida.getCriadoEm());
		assertNotNull(persistida.getAtualizadoEm());
	}

	@Test
	void deveImpedirDuasAvaliacoesDoMesmoClienteNoMesmoEstabelecimento() {
		var estabelecimento = criarEstabelecimento(
				"Barbearia Unicidade"
		);

		var cliente = criarCliente(
				"João Santos",
				"joao.avaliacao@teste.com"
		);

		criarAvaliacao(cliente, estabelecimento, 5);

		var duplicada = new Avaliacao();
		duplicada.setCliente(cliente);
		duplicada.setEstabelecimento(estabelecimento);
		duplicada.setNota(3);
		duplicada.setComentario("Segunda avaliação");

		assertThrows(
				DataIntegrityViolationException.class,
				() -> avaliacaoRepository.saveAndFlush(duplicada)
		);
	}

	@Test
	void deveIsolarAvaliacoesPorEstabelecimento() {
		var estabelecimentoA = criarEstabelecimento(
				"Barbearia A"
		);

		var estabelecimentoB = criarEstabelecimento(
				"Barbearia B"
		);

		var clienteA = criarCliente(
				"Maria",
				"maria.isolamento@teste.com"
		);

		var clienteB = criarCliente(
				"João",
				"joao.isolamento@teste.com"
		);

		criarAvaliacao(clienteA, estabelecimentoA, 5);
		criarAvaliacao(clienteB, estabelecimentoB, 2);

		sincronizarBanco();

		var avaliacoesA = avaliacaoPublicaService.listar(
				estabelecimentoA.getId()
		);

		var avaliacoesB = avaliacaoPublicaService.listar(
				estabelecimentoB.getId()
		);

		assertEquals(1, avaliacoesA.size());
		assertEquals(1, avaliacoesB.size());

		assertEquals("Maria", avaliacoesA.get(0).nomeCliente());
		assertEquals(5, avaliacoesA.get(0).nota());

		assertEquals("João", avaliacoesB.get(0).nomeCliente());
		assertEquals(2, avaliacoesB.get(0).nota());

		var resumoA = avaliacaoPublicaService.consultarResumo(
				estabelecimentoA.getId()
		);

		var resumoB = avaliacaoPublicaService.consultarResumo(
				estabelecimentoB.getId()
		);

		assertEquals(1, resumoA.totalAvaliacoes());
		assertEquals(1, resumoB.totalAvaliacoes());

		assertEquals(
				0,
				resumoA.mediaNotas().compareTo(
						new BigDecimal("5.00")
				)
		);

		assertEquals(
				0,
				resumoB.mediaNotas().compareTo(
						new BigDecimal("2.00")
				)
		);
	}

	@Test
	void deveRecalcularMediaAposEdicaoEExclusao() {
		var estabelecimento = criarEstabelecimento(
				"Barbearia Média"
		);

		var cliente1 = criarCliente(
				"Cliente Um",
				"cliente1.media@teste.com"
		);

		var cliente2 = criarCliente(
				"Cliente Dois",
				"cliente2.media@teste.com"
		);

		var cliente3 = criarCliente(
				"Cliente Três",
				"cliente3.media@teste.com"
		);

		criarAvaliacao(cliente1, estabelecimento, 5);
		criarAvaliacao(cliente2, estabelecimento, 4);
		criarAvaliacao(cliente3, estabelecimento, 3);

		Long estabelecimentoId = estabelecimento.getId();

		sincronizarBanco();

		// Situação inicial: notas 5, 4 e 3.
		var resumoInicial = avaliacaoPublicaService
				.consultarResumo(estabelecimentoId);

		assertEquals(3, resumoInicial.totalAvaliacoes());

		assertEquals(
				0,
				resumoInicial.mediaNotas().compareTo(
						new BigDecimal("4.00")
				)
		);

		// Cliente 3 altera sua nota de 3 para 5.
		avaliacaoService.editar(
				estabelecimentoId,
				new AvaliacaoRequestDTO(
						5,
						"Avaliação atualizada"
				),
				cliente3.getEmail()
		);

		sincronizarBanco();

		var resumoEditado = avaliacaoPublicaService
				.consultarResumo(estabelecimentoId);

		assertEquals(3, resumoEditado.totalAvaliacoes());

		assertEquals(
				0,
				resumoEditado.mediaNotas().compareTo(
						new BigDecimal("4.67")
				)
		);

		// Cliente 2 exclui sua avaliação de nota 4.
		avaliacaoService.excluir(
				estabelecimentoId,
				cliente2.getEmail()
		);

		sincronizarBanco();

		var resumoFinal = avaliacaoPublicaService
				.consultarResumo(estabelecimentoId);

		assertEquals(2, resumoFinal.totalAvaliacoes());

		assertEquals(
				0,
				resumoFinal.mediaNotas().compareTo(
						new BigDecimal("5.00")
				)
		);

		List<Avaliacao> restantes = avaliacaoRepository
				.findByEstabelecimentoIdOrderByCriadoEmDesc(
						estabelecimentoId
				);

		assertEquals(2, restantes.size());
		assertTrue(restantes.stream()
				.allMatch(avaliacao -> avaliacao.getNota() == 5));
	}
}
