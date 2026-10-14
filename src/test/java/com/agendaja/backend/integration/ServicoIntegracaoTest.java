
package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.service.EstabelecimentoService;
import com.agendaja.backend.service.ServicoService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ServicoIntegracaoTest {

	@Autowired
	private EstabelecimentoService estabelecimentoService;

	@Autowired
	private ServicoService servicoService;

	@Autowired
	private ServicoRepository servicoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private EstabelecimentoAuthorizationService authorizationService;

	@Test
	void deveCriarEditarEPersistirServico() {

		String emailGestor = "gestor.servico@teste.com";

		var cadastro = new EstabelecimentoCadastroDTO(
				"Barbearia Integração",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"Gestor Teste",
				emailGestor,
				"SenhaSegura123"
		);

		estabelecimentoService.cadastrarEstabelecimentoEGestor(
				cadastro
		);

		var gestor = usuarioRepository.findByEmail(emailGestor)
				.orElseThrow();

		Long estabelecimentoId =
				gestor.getEstabelecimento().getId();

		var dadosCriacao = new ServicoRequestDTO(
				"Corte de cabelo",
				30,
				new BigDecimal("35.00")
		);

		var criado = servicoService.criar(
				estabelecimentoId,
				dadosCriacao,
				emailGestor
		);

		assertNotNull(criado.id());

		// Força a escrita no PostgreSQL e limpa o cache JPA
		entityManager.flush();
		entityManager.clear();

		var persistido = servicoRepository.findById(criado.id())
				.orElseThrow();

		assertEquals("Corte de cabelo", persistido.getNome());
		assertEquals(30, persistido.getDuracaoMinutos());
		assertEquals(
				0,
				persistido.getValor().compareTo(
						new BigDecimal("35.00")
				)
		);

		var dadosEdicao = new ServicoRequestDTO(
				"Corte e barba",
				45,
				new BigDecimal("50.00")
		);

		servicoService.editar(
				estabelecimentoId,
				criado.id(),
				dadosEdicao,
				emailGestor
		);

		// Força a atualização e descarta entidades em memória
		entityManager.flush();
		entityManager.clear();

		var atualizado = servicoRepository.findById(criado.id())
				.orElseThrow();

		assertEquals("Corte e barba", atualizado.getNome());
		assertEquals(45, atualizado.getDuracaoMinutos());

		assertEquals(
				0,
				atualizado.getValor().compareTo(
						new BigDecimal("50.00")
				)
		);

		// Desativar o serviço
		servicoService.desativar(
				estabelecimentoId,
				criado.id(),
				emailGestor
		);

		entityManager.flush();
		entityManager.clear();

		var desativado = servicoRepository.findById(criado.id())
				.orElseThrow();

		assertFalse(desativado.isAtivo());

		var servicosAtivos = servicoRepository
				.findByEstabelecimentoIdAndAtivoTrue(estabelecimentoId);

		assertTrue(servicosAtivos.stream()
				.noneMatch(servico -> servico.getId().equals(criado.id())));
	}
}
