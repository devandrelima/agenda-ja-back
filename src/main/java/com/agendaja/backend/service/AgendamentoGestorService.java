package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AgendamentoGestorService {
    private final AgendamentoRepository agendamentoRepository;
    private final EstabelecimentoAuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void alterarStatus(Long estabelecimentoId, Long agendamentoId, StatusAgendamento novoStatus, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado."));

        if (!agendamento.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não pertence a este estabelecimento.");

        StatusAgendamento statusAnterior = agendamento.getStatus();

        // O Enum decide se a transição é válida
        if (!statusAnterior.podeTransitarPara(novoStatus))
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Não é possível alterar o status de " + statusAnterior + " para " + novoStatus + "."
            );

        agendamento.setStatus(novoStatus);
        
        // Dispara o evento de forma assíncrona após o commit para o envio de e-mails/WhatsApp futuros
        eventPublisher.publishEvent(new AgendamentoStatusAlteradoEvent(agendamento, statusAnterior));
    }
}
