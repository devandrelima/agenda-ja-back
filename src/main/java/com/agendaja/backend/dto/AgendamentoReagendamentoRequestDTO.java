package com.agendaja.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendamentoReagendamentoRequestDTO(

		@NotNull(message = "O estabelecimento é obrigatório")
		Long estabelecimentoId,

		@NotNull(message = "O serviço é obrigatório")
		Long servicoId,

		@NotNull(message = "O profissional é obrigatório")
		Long profissionalId,

		@NotNull(message = "A nova data e hora são obrigatórias")
		LocalDateTime dataHoraInicio

) {}
