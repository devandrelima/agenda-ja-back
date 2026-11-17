package com.agendaja.backend.repository;

import java.math.BigDecimal;

public interface RelatorioOperacionalProjection {

	Long getTotalConcluidos();

	BigDecimal getFaturamentoConcluidos();

	Long getTotalConfirmados();

	Long getTotalConsideradosNaTaxa();
}