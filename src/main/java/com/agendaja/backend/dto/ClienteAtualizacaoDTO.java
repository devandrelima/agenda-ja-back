package com.agendaja.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ClienteAtualizacaoDTO(
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
        String email
) {
    public ClienteAtualizacaoDTO
    {
        if (nome != null) nome = nome.strip();
        if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
        if (telefone != null) telefone = telefone.replaceAll("[\\s()\\-]", "");
    }
}
