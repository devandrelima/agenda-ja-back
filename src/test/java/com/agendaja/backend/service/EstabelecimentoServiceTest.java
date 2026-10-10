
package com.agendaja.backend.service;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoServiceTest {

	@Mock
	private EstabelecimentoRepository estabelecimentoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private EstabelecimentoService estabelecimentoService;

	@Test
	void naoDeveCadastrarEstabelecimentoQuandoEmailJaExiste() {

		var dto = new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				"joao@email.com",
				"SenhaSegura123"
		);

		when(usuarioRepository.existsByEmail("joao@email.com"))
				.thenReturn(true);

		var exception = assertThrows(
				EmailJaCadastradoException.class,
				() -> estabelecimentoService
						.cadastrarEstabelecimentoEGestor(dto)
		);

		assertEquals(
				"E-mail já cadastrado no sistema.",
				exception.getMessage()
		);

		verify(usuarioRepository)
				.existsByEmail("joao@email.com");

		verify(estabelecimentoRepository, never())
				.save(any());

		verify(usuarioRepository, never())
				.save(any());
	}


	@Test
	void deveCadastrarEstabelecimentoEGestorComSucesso() {

		var dto = new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				"joao@email.com",
				"SenhaSegura123"
		);

		var estabelecimentoSalvo = new Estabelecimento(
				1L,
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		when(usuarioRepository.existsByEmail("joao@email.com"))
				.thenReturn(false);

		when(estabelecimentoRepository.save(any(Estabelecimento.class)))
				.thenReturn(estabelecimentoSalvo);

		when(passwordEncoder.encode("SenhaSegura123"))
				.thenReturn("hash-da-senha");

		estabelecimentoService.cadastrarEstabelecimentoEGestor(dto);

		ArgumentCaptor<Estabelecimento> estabelecimentoCaptor =
				ArgumentCaptor.forClass(Estabelecimento.class);

		verify(estabelecimentoRepository)
				.save(estabelecimentoCaptor.capture());

		var estabelecimentoCriado = estabelecimentoCaptor.getValue();

		assertEquals("Barbearia Teste", estabelecimentoCriado.getNome());
		assertEquals("BARBEARIA", estabelecimentoCriado.getCategoria());

		ArgumentCaptor<Usuario> usuarioCaptor =
				ArgumentCaptor.forClass(Usuario.class);

		verify(usuarioRepository)
				.save(usuarioCaptor.capture());

		var gestorCriado = usuarioCaptor.getValue();

		assertEquals("João Silva", gestorCriado.getNome());
		assertEquals("joao@email.com", gestorCriado.getEmail());
		assertEquals("GESTOR", gestorCriado.getRole());
		assertEquals("hash-da-senha", gestorCriado.getSenha());

		assertSame(
				estabelecimentoSalvo,
				gestorCriado.getEstabelecimento()
		);

		verify(passwordEncoder).encode("SenhaSegura123");
	}


	@Test
	void deveNormalizarEmailAntesDeConsultarESalvar() {

		var dto = new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				"  JOAO@EMAIL.COM  ",
				"SenhaSegura123"
		);

		var estabelecimentoSalvo = new Estabelecimento(
				1L,
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		when(estabelecimentoRepository.save(any(Estabelecimento.class)))
				.thenReturn(estabelecimentoSalvo);

		when(passwordEncoder.encode(anyString()))
				.thenReturn("hash-da-senha");

		estabelecimentoService.cadastrarEstabelecimentoEGestor(dto);

		verify(usuarioRepository)
				.existsByEmail("joao@email.com");

		ArgumentCaptor<Usuario> usuarioCaptor =
				ArgumentCaptor.forClass(Usuario.class);

		verify(usuarioRepository)
				.save(usuarioCaptor.capture());

		assertEquals(
				"joao@email.com",
				usuarioCaptor.getValue().getEmail()
		);
	}


	@Test
	void devePropagarErroAoSalvarGestor() {

		var dto = new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				"joao@email.com",
				"SenhaSegura123"
		);

		var estabelecimentoSalvo = new Estabelecimento(
				1L,
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		when(estabelecimentoRepository.save(any(Estabelecimento.class)))
				.thenReturn(estabelecimentoSalvo);

		when(passwordEncoder.encode(anyString()))
				.thenReturn("hash-da-senha");

		when(usuarioRepository.save(any(Usuario.class)))
				.thenThrow(new RuntimeException("Erro ao salvar gestor"));

		var exception = assertThrows(
				RuntimeException.class,
				() -> estabelecimentoService
						.cadastrarEstabelecimentoEGestor(dto)
		);

		assertEquals(
				"Erro ao salvar gestor",
				exception.getMessage()
		);

		verify(estabelecimentoRepository).save(any());
		verify(usuarioRepository).save(any());
	}

}
