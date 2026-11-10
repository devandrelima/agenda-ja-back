package com.agendaja.backend.listener;

import com.agendaja.backend.adapter.NotificadorService;
import com.agendaja.backend.event.AgendamentoCriadoEvent;
import com.agendaja.backend.event.AgendamentoReagendadoEvent;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import com.agendaja.backend.model.StatusAgendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificadorAgendamentoListener {

    private static final Logger log = LoggerFactory.getLogger(NotificadorAgendamentoListener.class);
    private final NotificadorService notificadorService;

    public NotificadorAgendamentoListener(NotificadorService notificadorService) {
        this.notificadorService = notificadorService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        log.info(
                "OBSERVER ACIONADO: Disparando e-mail de solicitação para o agendamento {} do cliente {}",
                event.agendamento().getId(),
                event.agendamento().getCliente().getNome()
        );
        notificadorService.enviarSolicitacaoAgendamento(event.agendamento());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusAlterado(AgendamentoStatusAlteradoEvent event) {
        log.info("OBSERVER ACIONADO: Status do agendamento {} alterado de {} para {} (Cliente: {})",
                event.agendamento().getId(),
                event.statusAnterior(),
                event.agendamento().getStatus(),
                event.agendamento().getCliente().getNome()
        );

        // Dispara a confirmação definitiva apenas se o novo status for CONFIRMADO
        if (event.agendamento().getStatus() == StatusAgendamento.CONFIRMADO) {
            notificadorService.enviarConfirmacaoAgendamento(event.agendamento());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoReagendado(AgendamentoReagendadoEvent event) {
        log.info("OBSERVER ACIONADO: Agendamento {} reagendado de {} para {} (Cliente: {})",
                event.agendamento().getId(),
                event.dataHoraInicioAnterior(),
                event.agendamento().getDataHoraInicio(),
                event.agendamento().getCliente().getNome()
        );
    }
}