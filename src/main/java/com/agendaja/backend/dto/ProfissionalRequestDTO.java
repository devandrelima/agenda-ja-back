package com.agendaja.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProfissionalRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,
        
        @NotEmpty(message = "Selecione ao menos uma especialidade")
        List<String> especialidades,
        
        @NotEmpty(message = "Selecione ao menos um serviço")
        List<Long> idsServicos,
        
        @NotEmpty(message = "Selecione ao menos um dia de atendimento")
        List<Integer> diasDeAtendimento,
        
        @NotNull(message = "Dias de folga não podem ser nulos")
        List<String> diasDeFolga
) {
    public ProfissionalRequestDTO
    {
        if (nome != null) nome = nome.strip();
    }
}
