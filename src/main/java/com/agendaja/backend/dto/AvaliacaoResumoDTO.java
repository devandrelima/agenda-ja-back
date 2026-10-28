package com.agendaja.backend.dto;

import java.math.BigDecimal;

public record AvaliacaoResumoDTO(
		Long estabelecimentoId,
		long totalAvaliacoes,
		BigDecimal mediaNotas
) {}
