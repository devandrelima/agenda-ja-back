package com.agendaja.backend.dto;

import com.agendaja.backend.model.Agendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoClienteDTO(
		Long id,
		Long estabelecimentoId,
		UUID estabelecimentoPublicId,
		String estabelecimentoNome,
		String estabelecimentoLocalizacao,
		Long servicoId,
		String servicoNome,
		Long profissionalId,
		String profissionalNome,
		LocalDateTime dataHoraInicio,
		LocalDateTime dataHoraFim,
		String status,
		BigDecimal valor
) {

	public static AgendamentoClienteDTO from(
			Agendamento agendamento
	) {
		var profissional = agendamento.getProfissional();
		var estabelecimento = profissional.getEstabelecimento();
		var servico = agendamento.getServico();

		return new AgendamentoClienteDTO(
				agendamento.getId(),
				estabelecimento.getId(),
				estabelecimento.getPublicId(),
				estabelecimento.getNome(),
				estabelecimento.getLocalizacao(),
				servico.getId(),
				servico.getNome(),
				profissional.getId(),
				profissional.getNome(),
				agendamento.getDataHoraInicio(),
				agendamento.getDataHoraFim(),
				agendamento.getStatus().name(),
				servico.getValor()
		);
	}
}
