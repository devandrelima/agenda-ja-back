package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoHistoricoDTO(
        Long id,
        String servicoNome,
        String profissionalNome,
        LocalDateTime dataHoraInicio,
        String status,
        BigDecimal valor
) {
    public static AgendamentoHistoricoDTO from(Agendamento agendamento)
    {
        return new AgendamentoHistoricoDTO(
                agendamento.getId(),
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getStatus().name(),
                agendamento.getServico().getValor()
        );
    }
}
