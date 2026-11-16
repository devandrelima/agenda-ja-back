package com.agendaja.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResumoDTO(
		LocalDate data,
		Long totalReservas,
		Long confirmados,
		Long pendentes,
		Long cancelados,
		Long concluidos,
		BigDecimal faturamentoPrevisto,
		List<ProximoAtendimentoDTO> proximosAtendimentos
) {
}