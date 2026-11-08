package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;
import java.time.LocalDateTime;

public record AgendamentoAgendaDTO(
        Long id,
        String clienteNome,
        String servicoNome,
        Long profissionalId,
        String profissionalNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        String status
) {
    public static AgendamentoAgendaDTO from(Agendamento agendamento)
    {
        return new AgendamentoAgendaDTO(
                agendamento.getId(),
                agendamento.getCliente().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getId(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus().name()
        );
    }
}
