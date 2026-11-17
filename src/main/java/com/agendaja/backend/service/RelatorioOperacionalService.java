package com.agendaja.backend.service;

import com.agendaja.backend.dto.RelatorioOperacionalDTO;
import com.agendaja.backend.dto.ServicoPopularDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioOperacionalService {

	private final AgendamentoRepository agendamentoRepository;
	private final EstabelecimentoAuthorizationService authorizationService;

	@Transactional(readOnly = true)
	public RelatorioOperacionalDTO consultarRelatorio(
			Long estabelecimentoId,
			LocalDate dataInicio,
			LocalDate dataFim,
			String emailGestor
	) {
		authorizationService.validarAcesso(emailGestor, estabelecimentoId);

		validarPeriodo(dataInicio, dataFim);

		var inicio = dataInicio.atStartOfDay();
		var fim = dataFim.plusDays(1).atStartOfDay();

		var indicadores = agendamentoRepository.buscarIndicadoresRelatorio(
				estabelecimentoId,
				inicio,
				fim,
				StatusAgendamento.CONCLUIDO,
				StatusAgendamento.CONFIRMADO,
				StatusAgendamento.CANCELADO
		);

		BigDecimal taxaConfirmacao = calcularTaxaConfirmacao(
				indicadores.getTotalConfirmados(),
				indicadores.getTotalConsideradosNaTaxa()
		);

		List<ServicoPopularDTO> servicosPopulares =
				agendamentoRepository.buscarServicosMaisPopulares(
								estabelecimentoId,
								inicio,
								fim,
								StatusAgendamento.CANCELADO
						)
						.stream()
						.map(servico -> new ServicoPopularDTO(
								servico.getServicoId(),
								servico.getServicoNome(),
								servico.getQuantidadeAgendamentos()
						))
						.toList();

		return new RelatorioOperacionalDTO(
				dataInicio,
				dataFim,
				indicadores.getTotalConcluidos(),
				indicadores.getFaturamentoConcluidos(),
				taxaConfirmacao,
				servicosPopulares
		);
	}

	private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
		if (dataInicio == null || dataFim == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"As datas inicial e final são obrigatórias."
			);
		}

		if (dataInicio.isAfter(dataFim)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"A data inicial não pode ser posterior à data final."
			);
		}
	}

	private BigDecimal calcularTaxaConfirmacao(
			Long totalConfirmados,
			Long totalConsiderados
	) {
		if (totalConsiderados == null || totalConsiderados == 0) {
			return BigDecimal.ZERO.setScale(2);
		}

		return BigDecimal.valueOf(totalConfirmados)
				.multiply(BigDecimal.valueOf(100))
				.divide(
						BigDecimal.valueOf(totalConsiderados),
						2,
						RoundingMode.HALF_UP
				);
	}
}
