
package com.agendaja.backend.integration;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.service.EstabelecimentoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class IntegridadeCadastroTest {

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

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@AfterEach
	void limparDados() {
		usuarioRepository.deleteAll();
		estabelecimentoRepository.deleteAll();
	}

	@Test
	void bancoDeveImpedirEmailDuplicado() {

		var primeiroUsuario = new Usuario(
				null,
				null,
				"João Silva",
				"duplicado@email.com",
				"hash-teste",
				"CLIENTE"
		);

		var segundoUsuario = new Usuario(
				null,
				null,
				"Maria Silva",
				"duplicado@email.com",
				"hash-teste",
				"CLIENTE"
		);

		usuarioRepository.saveAndFlush(primeiroUsuario);

		assertThrows(
				DataIntegrityViolationException.class,
				() -> usuarioRepository.saveAndFlush(segundoUsuario)
		);

		assertEquals(1, usuarioRepository.count());
	}

	@Test
	void deveDesfazerCadastroQuandoOcorreFalha() {

		var dto = new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				"gestor@email.com",
				"SenhaSegura123"
		);

		when(passwordEncoder.encode("SenhaSegura123"))
				.thenThrow(
						new IllegalStateException(
								"Falha simulada no processamento da senha"
						)
				);

		assertThrows(
				IllegalStateException.class,
				() -> estabelecimentoService
						.cadastrarEstabelecimentoEGestor(dto)
		);

		assertEquals(0, estabelecimentoRepository.count());
		assertEquals(0, usuarioRepository.count());

		verify(passwordEncoder).encode("SenhaSegura123");
	}
}
