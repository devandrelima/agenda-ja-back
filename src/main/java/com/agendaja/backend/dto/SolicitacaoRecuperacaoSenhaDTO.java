package com.agendaja.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SolicitacaoRecuperacaoSenhaDTO(

		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "Formato de e-mail inválido")
		@Size(
				max = 100,
				message = "O e-mail deve ter no máximo 100 caracteres"
		)
		String email

) {
	public SolicitacaoRecuperacaoSenhaDTO {
		if (email != null) {
			email = email.strip().toLowerCase(Locale.ROOT);
		}
	}
}
