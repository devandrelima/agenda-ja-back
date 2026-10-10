
package com.agendaja.backend.integration;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.service.EstabelecimentoService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class EstabelecimentoIntegracaoTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres =
			new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private EstabelecimentoService estabelecimentoService;

	@Autowired
	private EstabelecimentoRepository estabelecimentoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private EstabelecimentoCadastroDTO criarDto(String email) {
		return new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				email,
				"SenhaSegura123"
		);
	}

	@Test
	void devePersistirEstabelecimentoEGestor() {

		var dto = criarDto("gestor1@email.com");

		estabelecimentoService.cadastrarEstabelecimentoEGestor(dto);

		var estabelecimentos = estabelecimentoRepository.findAll();
		var usuarios = usuarioRepository.findAll();

		assertEquals(1, estabelecimentos.size());
		assertEquals(1, usuarios.size());

		var estabelecimento = estabelecimentos.get(0);
		var gestor = usuarios.get(0);

		assertNotNull(estabelecimento.getId());
		assertNotNull(gestor.getId());

		assertEquals("Barbearia Teste", estabelecimento.getNome());
		assertEquals("GESTOR", gestor.getRole());
		assertEquals("gestor1@email.com", gestor.getEmail());

		assertEquals(
				estabelecimento.getId(),
				gestor.getEstabelecimento().getId()
		);
	}

	@Test
	void deveArmazenarSenhaComBCrypt() {

		var dto = criarDto("gestor2@email.com");

		estabelecimentoService.cadastrarEstabelecimentoEGestor(dto);

		var gestor = usuarioRepository.findAll().get(0);

		assertNotEquals("SenhaSegura123", gestor.getSenha());

		assertTrue(
				passwordEncoder.matches(
						"SenhaSegura123",
						gestor.getSenha()
				)
		);
	}

	@Test
	void deveImpedirEmailDuplicado() {

		estabelecimentoService.cadastrarEstabelecimentoEGestor(
				criarDto("duplicado@email.com")
		);

		assertThrows(
				EmailJaCadastradoException.class,
				() -> estabelecimentoService
						.cadastrarEstabelecimentoEGestor(
								criarDto("duplicado@email.com")
						)
		);

		assertEquals(1, estabelecimentoRepository.count());
		assertEquals(1, usuarioRepository.count());
	}

	@Test
	void devePermitirClienteSemEstabelecimento() {

		var cliente = new Usuario(
				null,
				null,
				"Maria Silva",
				"cliente@email.com",
				passwordEncoder.encode("SenhaCliente123"),
				"CLIENTE"
		);

		var clienteSalvo = usuarioRepository.saveAndFlush(cliente);

		assertNotNull(clienteSalvo.getId());
		assertNull(clienteSalvo.getEstabelecimento());
		assertEquals("CLIENTE", clienteSalvo.getRole());
	}
}
