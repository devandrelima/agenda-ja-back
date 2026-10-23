package com.agendaja.backend.listener;

import com.agendaja.backend.event.AgendamentoCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificadorAgendamentoListener {
    private static final Logger log = LoggerFactory.getLogger(NotificadorAgendamentoListener.class);

    // Ouve o evento apenas APÓS o commit no banco de dados ser realizado com sucesso
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event)
    {
        // TO-DO: Aplicar o padrão Adapter futuramente para disparar o WhatsApp/E-mail.
        // Por enquanto, apenas registra o log provando que o Observer funciona de forma autônoma.
        log.info(
            "OBSERVER ACIONADO: Disparando notificações para o agendamento {} do cliente {}", 
            event.agendamento().getId(), 
            event.agendamento().getCliente().getNome()
        );
    }
}
