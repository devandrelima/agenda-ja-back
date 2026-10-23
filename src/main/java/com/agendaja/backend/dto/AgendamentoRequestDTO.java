package com.agendaja.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AgendamentoRequestDTO(
        @NotNull(message = "O serviço é obrigatório") 
        Long servicoId,
        
        @NotNull(message = "O profissional é obrigatório") 
        Long profissionalId,
        
        @NotNull(message = "A data e hora de início são obrigatórias") 
        LocalDateTime dataHoraInicio,
        
        String observacoes
) {}
