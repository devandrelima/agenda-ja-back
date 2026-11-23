package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AgendamentoAgendaDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.service.AgendamentoGestorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgendaControllerTest {
    @Mock private AgendamentoGestorService agendamentoGestorService;
    @InjectMocks private AdminAgendaController controller;

    @Test
    void deveDelegarConsultaDaAgendaParaOServico() {
        var inicio = LocalDateTime.of(2026, 12, 1, 0, 0);
        var fim = LocalDateTime.of(2026, 12, 7, 23, 59);
        var atualizadoApos = LocalDateTime.of(2026, 12, 1, 10, 0);
        var status = List.of(StatusAgendamento.CONFIRMADO);
        var esperado = List.<AgendamentoAgendaDTO>of();

        var auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("gestor@teste.com");
        when(agendamentoGestorService.listarAgenda(1L, inicio, fim, 2L, status, atualizadoApos, "gestor@teste.com"))
                .thenReturn(esperado);

        var resultado = controller.consultarAgenda(1L, inicio, fim, 2L, status, atualizadoApos, auth);

        assertSame(esperado, resultado);
        verify(agendamentoGestorService).listarAgenda(1L, inicio, fim, 2L, status, atualizadoApos, "gestor@teste.com");
    }
}
