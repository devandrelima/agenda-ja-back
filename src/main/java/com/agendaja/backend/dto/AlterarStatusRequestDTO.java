package com.agendaja.backend.dto;

import com.agendaja.backend.model.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequestDTO(
    @NotNull(message = "O novo status é obrigatório.")
    StatusAgendamento status
) {}
