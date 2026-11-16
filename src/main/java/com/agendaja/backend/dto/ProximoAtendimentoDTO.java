package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProximoAtendimentoDTO(
		Long agendamentoId,
		LocalDateTime horarioInicio,
		String clienteNome,
		String servicoNome,
		String profissionalNome,
		String status,
		BigDecimal valorPrevisto
) {
	public static ProximoAtendimentoDTO from(Agendamento agendamento) {
		return new ProximoAtendimentoDTO(
				agendamento.getId(),
				agendamento.getDataHoraInicio(),
				agendamento.getCliente().getNome(),
				agendamento.getServico().getNome(),
				agendamento.getProfissional().getNome(),
				agendamento.getStatus().name(),
				agendamento.getServico().getValor()
		);
	}
}