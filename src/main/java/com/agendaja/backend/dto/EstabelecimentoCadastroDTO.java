package com.agendaja.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstabelecimentoCadastroDTO(
        @NotBlank(message = "O nome do negócio é obrigatório")
        String nomeNegocio,

        @NotBlank(message = "A categoria é obrigatória")
        String categoria,

        String imagemUrl,

        @NotBlank(message = "A localização é obrigatória")
        String localizacao,

        @NotBlank(message = "O nome do gestor é obrigatório")
        String nomeGestor,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String senha
) {
        public EstabelecimentoCadastroDTO {
            if (email!= null) {
                email = email.strip();
            }
        }
}