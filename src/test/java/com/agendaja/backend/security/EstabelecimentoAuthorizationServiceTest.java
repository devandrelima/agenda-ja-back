package com.agendaja.backend.security;

import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoAuthorizationServiceTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstabelecimentoAuthorizationService service;

    @Test
    void devePermitirAcessoQuandoGestorPertenceAoEstabelecimento() {
        var est = new Estabelecimento(1L, "Salão", "SALAO", null, "Centro");
        var gestor = new Usuario(10L, est, "Gestor", "gestor@teste.com", "hash", "GESTOR");
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestor));

        assertDoesNotThrow(() -> service.validarAcesso("gestor@teste.com", 1L));
    }

    @Test
    void deveNegarAcessoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.validarAcesso("inexistente@teste.com", 1L));
    }

    @Test
    void deveNegarAcessoQuandoUsuarioNaoEGestor() {
        var est = new Estabelecimento(1L, "Salão", "SALAO", null, "Centro");
        var cliente = new Usuario(10L, est, "Cliente", "cliente@teste.com", "hash", "CLIENTE");
        when(usuarioRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.of(cliente));

        assertThrows(AccessDeniedException.class,
                () -> service.validarAcesso("cliente@teste.com", 1L));
    }

    @Test
    void deveNegarAcessoQuandoGestorNaoPossuiEstabelecimentoVinculado() {
        var gestorSemEst = new Usuario(10L, null, "Gestor", "gestor@teste.com", "hash", "GESTOR");
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestorSemEst));

        assertThrows(AccessDeniedException.class,
                () -> service.validarAcesso("gestor@teste.com", 1L));
    }

    @Test
    void deveNegarAcessoQuandoGestorPertenceAOutroEstabelecimento() {
        var outroEst = new Estabelecimento(2L, "Outro Salão", "SALAO", null, "Centro");
        var gestor = new Usuario(10L, outroEst, "Gestor", "gestor@teste.com", "hash", "GESTOR");
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestor));

        assertThrows(AccessDeniedException.class,
                () -> service.validarAcesso("gestor@teste.com", 1L));
    }
}
