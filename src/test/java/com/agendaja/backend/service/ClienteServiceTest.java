package com.agendaja.backend.service;

import com.agendaja.backend.dto.ClienteCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private ClienteService clienteService;

	private ClienteCadastroDTO criarDto() {
		return new ClienteCadastroDTO(
				"Maria Silva",
				"(84) 99999-9999",
				"MARIA@EMAIL.COM",
				"SenhaSegura123"
		);
	}

	@Test
	void deveCadastrarClienteComSucesso() {

		var dto = criarDto();

		when(passwordEncoder.encode("SenhaSegura123"))
				.thenReturn("senha-hasheada");

		when(usuarioRepository.save(any(Usuario.class)))
				.thenAnswer(invocation -> {
					Usuario usuario = invocation.getArgument(0);
					usuario.setId(1L);
					return usuario;
				});

		var resposta = clienteService.cadastrar(dto);

		ArgumentCaptor<Usuario> captor =
				ArgumentCaptor.forClass(Usuario.class);

		verify(usuarioRepository).save(captor.capture());

		Usuario cliente = captor.getValue();

		assertEquals("Maria Silva", cliente.getNome());
		assertEquals("84999999999", cliente.getTelefone());
		assertEquals("maria@email.com", cliente.getEmail());
		assertEquals("CLIENTE", cliente.getRole());
		assertNull(cliente.getEstabelecimento());

		assertEquals(1L, resposta.id());
		assertEquals("Maria Silva", resposta.nome());
		assertEquals("84999999999", resposta.telefone());
		assertEquals("maria@email.com", resposta.email());
	}

	@Test
	void deveAplicarHashNaSenhaAntesDeSalvar() {

		var dto = criarDto();

		when(passwordEncoder.encode("SenhaSegura123"))
				.thenReturn("hash-gerado");

		when(usuarioRepository.save(any(Usuario.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		clienteService.cadastrar(dto);

		ArgumentCaptor<Usuario> captor =
				ArgumentCaptor.forClass(Usuario.class);

		verify(usuarioRepository).save(captor.capture());

		assertEquals("hash-gerado", captor.getValue().getSenha());
		assertNotEquals("SenhaSegura123", captor.getValue().getSenha());

		verify(passwordEncoder).encode("SenhaSegura123");
	}

	@Test
	void deveRejeitarEmailDuplicado() {

		var dto = criarDto();

		when(usuarioRepository.existsByEmail("maria@email.com"))
				.thenReturn(true);

		var excecao = assertThrows(
				EmailJaCadastradoException.class,
				() -> clienteService.cadastrar(dto)
		);

		assertEquals(
				"E-mail já cadastrado no sistema.",
				excecao.getMessage()
		);

		verify(usuarioRepository).existsByEmail("maria@email.com");
	}

	@Test
	void naoDeveSalvarNemCodificarSenhaQuandoEmailJaExiste() {

		var dto = criarDto();

		when(usuarioRepository.existsByEmail("maria@email.com"))
				.thenReturn(true);

		assertThrows(
				EmailJaCadastradoException.class,
				() -> clienteService.cadastrar(dto)
		);

		verify(usuarioRepository, never()).save(any(Usuario.class));
		verifyNoInteractions(passwordEncoder);
	}
}
