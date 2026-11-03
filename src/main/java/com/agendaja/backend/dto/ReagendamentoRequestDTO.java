package com.agendaja.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReagendamentoRequestDTO(
    @NotNull(message = "O novo horário de início é obrigatório.")
    LocalDateTime dataHoraInicio
) {}
