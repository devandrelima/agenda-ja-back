package com.agendaja.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequestDTO(

		@NotNull(message = "A nota é obrigatória")
		@Min(value = 1, message = "A nota mínima é 1")
		@Max(value = 5, message = "A nota máxima é 5")
		Integer nota,

		@NotBlank(message = "O comentário é obrigatório")
		@Size(
				max = 1000,
				message = "O comentário deve ter no máximo 1000 caracteres"
		)
		String comentario

) {
	public AvaliacaoRequestDTO {
		if (comentario != null) {
			comentario = comentario.strip();
		}
	}
}
