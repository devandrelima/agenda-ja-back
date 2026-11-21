package com.agendaja.backend.listener;

import com.agendaja.backend.adapter.NotificadorService;
import com.agendaja.backend.event.AgendamentoCriadoEvent;
import com.agendaja.backend.event.AgendamentoReagendadoEvent;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.service.WahaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificadorAgendamentoListenerTest {
    @Mock private NotificadorService notificadorService;
    @Mock private WahaService wahaService;
    @InjectMocks private NotificadorAgendamentoListener listener;

    private Agendamento agendamento;

    @BeforeEach
    void setup() {
        var cliente = new Usuario(1L, null, "Maria Silva", "maria@teste.com", "hash", "CLIENTE");
        agendamento = new Agendamento();
        agendamento.setId(50L);
        agendamento.setCliente(cliente);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 12, 10, 14, 0));
        agendamento.setDataHoraFim(LocalDateTime.of(2026, 12, 10, 14, 30));
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
    }

    @Test
    void deveEnviarSolicitacaoAoCriarAgendamento() {
        listener.onAgendamentoCriado(new AgendamentoCriadoEvent(agendamento));

        verify(notificadorService).enviarSolicitacaoAgendamento(agendamento);
    }

    @Test
    void deveEnviarConfirmacaoQuandoNovoStatusForConfirmado() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);

        listener.onStatusAlterado(new AgendamentoStatusAlteradoEvent(
                agendamento, StatusAgendamento.AGUARDANDO_CONFIRMACAO));

        verify(notificadorService).enviarConfirmacaoAgendamento(agendamento);
    }

    @Test
    void naoDeveEnviarConfirmacaoQuandoNovoStatusNaoForConfirmado() {
        agendamento.setStatus(StatusAgendamento.CANCELADO);

        listener.onStatusAlterado(new AgendamentoStatusAlteradoEvent(
                agendamento, StatusAgendamento.CONFIRMADO));

        verifyNoInteractions(notificadorService);
    }

    @Test
    void deveProcessarEventoDeReagendamentoSemErro() {
        var inicioAnterior = LocalDateTime.of(2026, 12, 9, 10, 0);
        var fimAnterior = LocalDateTime.of(2026, 12, 9, 10, 30);

        listener.onAgendamentoReagendado(new AgendamentoReagendadoEvent(
                agendamento, inicioAnterior, fimAnterior));

        verifyNoInteractions(notificadorService);
    }
}
