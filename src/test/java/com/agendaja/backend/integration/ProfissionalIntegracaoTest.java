package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.service.EstabelecimentoService;
import com.agendaja.backend.service.ProfissionalService;
import com.agendaja.backend.service.ServicoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Sem @Transactional de propósito: a sessão do Hibernate precisa fechar ao fim de cada
// chamada ao service, como acontece numa requisição real (spring.jpa.open-in-view=false).
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProfissionalIntegracaoTest {
	@Autowired
	private EstabelecimentoService estabelecimentoService;

	@Autowired
	private ServicoService servicoService;

	@Autowired
	private ProfissionalService profissionalService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private String emailGestor;
	private Long estabelecimentoId;
	private Long servicoId;

	@BeforeEach
	void prepararEstabelecimento()
	{
		emailGestor = "gestor-" + UUID.randomUUID() + "@email.com";
		estabelecimentoService.cadastrarEstabelecimentoEGestor(new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				emailGestor,
				"SenhaSegura123"
		));
		estabelecimentoId = usuarioRepository.findByEmail(emailGestor).orElseThrow()
				.getEstabelecimento().getId();
		servicoId = servicoService.criar(
				estabelecimentoId,
				new ServicoRequestDTO("Corte", 30, new BigDecimal("50.00")),
				emailGestor
		).id();
	}

	private Long criarProfissional()
	{
		return profissionalService.criar(
				estabelecimentoId,
				new ProfissionalRequestDTO("Ana", List.of("Corte", "Barba"), List.of(servicoId), List.of(1, 2), List.of()),
				emailGestor
		).id();
	}

	@Test
	void deveListarProfissionaisComEspecialidadesForaDaTransacao()
	{
		criarProfissional();

		var profissionais = profissionalService.listar(estabelecimentoId, emailGestor);

		assertEquals(1, profissionais.size());
		assertEquals(List.of("Corte", "Barba"), profissionais.get(0).especialidades());
	}

	@Test
	void deveConsultarProfissionalComEspecialidadesForaDaTransacao()
	{
		var profissionalId = criarProfissional();

		var profissional = profissionalService.consultar(estabelecimentoId, profissionalId, emailGestor);

		assertEquals(List.of("Corte", "Barba"), profissional.especialidades());
	}
}
