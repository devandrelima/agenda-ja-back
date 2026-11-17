package com.agendaja.backend.dto;

public record ServicoPopularDTO(
		Long servicoId,
		String servicoNome,
		Long quantidadeAgendamentos
) {
}