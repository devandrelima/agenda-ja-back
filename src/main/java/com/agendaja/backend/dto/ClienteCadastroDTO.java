package com.agendaja.backend.dto;

import jakarta.validation.constraints.*;

import java.util.Locale;

public record ClienteCadastroDTO(

		@NotBlank(message = "O nome completo é obrigatório")
		@Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
		String nome,

		@NotBlank(message = "O telefone é obrigatório")
		@Pattern(
				regexp = "\\d{10,11}",
				message = "O telefone deve conter 10 ou 11 dígitos"
		)
		String telefone,

		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "Formato de e-mail inválido")
		@Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
		String email,

		@NotBlank(message = "A senha é obrigatória")
		@Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
		String senha

) {
	public ClienteCadastroDTO {
		if (nome != null) {
			nome = nome.strip();
		}

		if (email != null) {
			email = email.strip().toLowerCase(Locale.ROOT);
		}

		if (telefone != null) {
			telefone = telefone.replaceAll("[\\s()\\-]", "");
		}
	}
}
