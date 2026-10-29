package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoDetalhesDTO(
        Long id,
        String estabelecimentoNome,
        String servicoNome,
        String profissionalNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        String status,
        BigDecimal valor
) {
    public static AgendamentoDetalhesDTO from(Agendamento agendamento)
    {
        return new AgendamentoDetalhesDTO(
                agendamento.getId(),
                agendamento.getProfissional().getEstabelecimento().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getProfissional().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus().name(),
                agendamento.getServico().getValor()
        );
    }
}
