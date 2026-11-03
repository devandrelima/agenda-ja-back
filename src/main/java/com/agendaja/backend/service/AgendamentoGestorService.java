package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.event.AgendamentoReagendadoEvent;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import java.time.LocalDateTime;

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

    @Transactional
    public void reagendar(Long estabelecimentoId, Long agendamentoId, LocalDateTime novaDataHoraInicio, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado."));

        if (!agendamento.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não pertence a este estabelecimento.");

        // Não faz sentido reagendar algo que já terminou ou foi cancelado
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO || agendamento.getStatus() == StatusAgendamento.CONCLUIDO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível reagendar um atendimento cancelado ou concluído.");

        LocalDateTime inicioAnterior = agendamento.getDataHoraInicio();
        LocalDateTime fimAnterior = agendamento.getDataHoraFim();

        agendamento.setDataHoraInicio(novaDataHoraInicio);
        agendamento.setDataHoraFim(novaDataHoraInicio.plusMinutes(agendamento.getServico().getDuracaoMinutos()));

        try {
            // Força o envio do UPDATE para o banco. Se a restrição GiST (tsrange) detectar colisão, falhará aqui.
            agendamentoRepository.flush();
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O novo horário selecionado colide com outra reserva.");
        }

        // Dispara o evento de notificação de forma totalmente desacoplada
        eventPublisher.publishEvent(new AgendamentoReagendadoEvent(agendamento, inicioAnterior, fimAnterior));
    }
}
