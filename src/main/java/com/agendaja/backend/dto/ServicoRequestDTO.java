
package com.agendaja.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServicoRequestDTO(

		@NotBlank(message = "O nome é obrigatório")
		String nome,

		@NotNull(message = "A duração é obrigatória")
		@Min(value = 1, message = "A duração deve ser maior que zero")
		Integer duracaoMinutos,

		@NotNull(message = "O valor é obrigatório")
		@DecimalMin(
				value = "0.00",
				message = "O valor não pode ser negativo"
		)
		@Digits(
				integer = 8,
				fraction = 2,
				message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais"
		)
		BigDecimal valor

) {
	public ServicoRequestDTO {
		if (nome != null) {
			nome = nome.strip();
		}
	}
}
