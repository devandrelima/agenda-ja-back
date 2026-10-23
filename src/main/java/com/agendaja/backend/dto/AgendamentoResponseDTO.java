package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResponseDTO(
        Long id,
        Long servicoId,
        Long profissionalId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        String status,
        BigDecimal valor
) {
    public static AgendamentoResponseDTO from(Agendamento agendamento)
    {
        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getServico().getId(),
                agendamento.getProfissional().getId(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus().name(),
                agendamento.getServico().getValor()
        );
    }
}
