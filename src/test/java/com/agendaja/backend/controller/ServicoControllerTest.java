package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.service.ServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoControllerTest {
    @Mock private ServicoService servicoService;
    @InjectMocks private ServicoController controller;

    private Authentication auth;

    @BeforeEach
    void setup() {
        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("gestor@teste.com");
    }

    @Test
    void deveDelegarTodasAsOperacoesDeCrudParaOServico() {
        var req = mock(ServicoRequestDTO.class);
        var resp = mock(ServicoResponseDTO.class);
        var lista = List.of(resp);

        when(servicoService.criar(1L, req, "gestor@teste.com")).thenReturn(resp);
        when(servicoService.listar(1L, "gestor@teste.com")).thenReturn(lista);
        when(servicoService.consultar(1L, 10L, "gestor@teste.com")).thenReturn(resp);
        when(servicoService.editar(1L, 10L, req, "gestor@teste.com")).thenReturn(resp);

        assertSame(resp, controller.criar(1L, req, auth));
        assertSame(lista, controller.listar(1L, auth));
        assertSame(resp, controller.consultar(1L, 10L, auth));
        assertSame(resp, controller.editar(1L, 10L, req, auth));

        controller.desativar(1L, 10L, auth);
        verify(servicoService).desativar(1L, 10L, "gestor@teste.com");
    }
}
