package com.agendaja.backend.repository;

import java.math.BigDecimal;

public interface DashboardAgendamentoProjection {

	Long getTotalReservas();

	Long getConfirmados();

	Long getPendentes();

	Long getCancelados();

	Long getConcluidos();

	BigDecimal getFaturamentoPrevisto();
}