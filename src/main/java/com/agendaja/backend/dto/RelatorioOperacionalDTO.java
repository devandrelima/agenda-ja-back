package com.agendaja.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioOperacionalDTO(
		LocalDate dataInicio,
		LocalDate dataFim,
		Long totalConcluidos,
		BigDecimal faturamentoConcluidos,
		BigDecimal taxaConfirmacao,
		List<ServicoPopularDTO> servicosMaisPopulares
) {
}