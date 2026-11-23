package com.agendaja.backend.service;

import com.agendaja.backend.dto.ClienteCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@InjectMocks
	private ClienteService clienteService;

	private ClienteCadastroDTO criarDto()
	{
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

	@Test
    void deveConsultarPerfilDoClienteAutenticado() {
        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        cliente.setTelefone("84999998888");
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(cliente));

        var resposta = clienteService.consultarPerfil("maria@teste.com");

        assertEquals("Maria", resposta.nome());
        assertEquals("maria@teste.com", resposta.email());
    }

    @Test
    void deveRejeitarAcessoAoPerfilQuandoUsuarioNaoExisteOuNaoECliente() {
        when(usuarioRepository.findByEmail("ausente@teste.com")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> clienteService.consultarPerfil("ausente@teste.com")
        ).getStatusCode().value());

        var gestor = new Usuario(2L, null, "Gestor", "gestor@teste.com", "hash", "GESTOR");
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestor));
        assertEquals(403, assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> clienteService.consultarPerfil("gestor@teste.com")
        ).getStatusCode().value());
    }

    @Test
    void deveAtualizarPerfilMantendoEmailOuTrocandoParaEmailLivreERejeitarDuplicado() {
        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(cliente));

        // 1. Mantendo o mesmo e-mail
        var mesmoEmailDto = new com.agendaja.backend.dto.ClienteAtualizacaoDTO(
                "Maria Atualizada", "84999991111", "maria@teste.com", "data:image/jpeg;base64,abc"
        );
        var res1 = clienteService.atualizarPerfil(mesmoEmailDto, "maria@teste.com");
        assertEquals("Maria Atualizada", res1.nome());
        verify(usuarioRepository, never()).existsByEmail(anyString());

        // 2. Tentando trocar para e-mail já utilizado por outra conta
        var emailDuplicadoDto = new com.agendaja.backend.dto.ClienteAtualizacaoDTO(
                "Maria", "84999991111", "outro@teste.com", null
        );
        when(usuarioRepository.existsByEmail("outro@teste.com")).thenReturn(true);
        assertThrows(EmailJaCadastradoException.class,
                () -> clienteService.atualizarPerfil(emailDuplicadoDto, "maria@teste.com"));

        // 3. Trocando para e-mail livre
        var emailLivreDto = new com.agendaja.backend.dto.ClienteAtualizacaoDTO(
                "Maria Nova", "84999992222", "novo@teste.com", null
        );
        when(usuarioRepository.existsByEmail("novo@teste.com")).thenReturn(false);
        var res3 = clienteService.atualizarPerfil(emailLivreDto, "maria@teste.com");
        assertEquals("novo@teste.com", res3.email());
    }

    @Test
    void deveDesativarContaECancelarAgendamentosFuturos() {
        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(cliente));

        clienteService.desativarConta("maria@teste.com");

        assertFalse(cliente.isAtivo());
        verify(agendamentoRepository).cancelarAgendamentosFuturosDoCliente(
                1L,
                List.of(
                        com.agendaja.backend.model.StatusAgendamento.AGUARDANDO_CONFIRMACAO,
                        com.agendaja.backend.model.StatusAgendamento.CONFIRMADO
                ),
                com.agendaja.backend.model.StatusAgendamento.CANCELADO
        );
    }
}
