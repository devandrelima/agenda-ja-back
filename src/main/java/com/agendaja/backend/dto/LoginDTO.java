
package com.agendaja.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginDTO(

		@NotBlank(message = "O e-mail é obrigatório")
		// Aceita e-mail ou telefone. O formato é resolvido pelo serviço de autenticação.
		String email,

		@NotBlank(message = "A senha é obrigatória")
		String senha

) {
	public LoginDTO {
		if (email != null) {
			email = email.contains("@")
					? email.strip().toLowerCase(Locale.ROOT)
					: email.replaceAll("\\D", "");
		}
	}
}
