package com.agendaja.backend.job;

import com.agendaja.backend.adapter.NotificadorService;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.service.WahaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LembreteAgendamentoJob {
    private static final Logger log = LoggerFactory.getLogger(LembreteAgendamentoJob.class);

    private final AgendamentoRepository agendamentoRepository;
    private final NotificadorService notificadorService;
    private final WahaService wahaService;

    // A expressão Cron "0 0 8 * * *" significa: Às 08h 00m 00s, todos os dias
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processarLembretesDeAmanha()
    {
        log.info("Iniciando rotina automática de envio de lembretes...");

        LocalDate amanha = LocalDate.now().plusDays(1);
        LocalDateTime inicioAmanha = amanha.atStartOfDay();
        LocalDateTime fimAmanha = amanha.atTime(LocalTime.MAX);

        List<StatusAgendamento> statusValidos = List.of(
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.AGUARDANDO_CONFIRMACAO
        );

        List<Agendamento> elegiveis = agendamentoRepository.buscarElegiveisParaLembrete(inicioAmanha, fimAmanha, statusValidos);

        if (elegiveis.isEmpty()) {
            log.info("Nenhum lembrete a ser enviado para amanhã ({}).", amanha);
            return;
        }

        int enviados = 0;
        for (Agendamento agendamento : elegiveis) {
            try {
                notificadorService.enviarLembreteAgendamento(agendamento);
                wahaService.enviarLembrete(agendamento);
                
                // Marca como enviado. Como o método é @Transactional, 
                // o JPA fará o flush automático ao fim da execução.
                agendamento.setLembreteEnviado(true);
                enviados++;
            } catch (Exception e) {
                // Captura a exceção dentro do loop 
                // Assim, se o disparo para o Cliente A falhar, o Cliente B recebe na mesma.
                log.error("Erro inesperado ao processar lembrete para a reserva {}: {}", agendamento.getId(), e.getMessage());
            }
        }

        log.info("Rotina de lembretes finalizada. Enviados: {}/{}", enviados, elegiveis.size());
    }
}
