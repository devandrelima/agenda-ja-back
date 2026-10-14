package com.agendaja.backend.dto;

import com.agendaja.backend.model.Folga;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FolgaDTO(
        @NotNull(message = "A data da folga é obrigatória")
        LocalDate data,

        String motivo
) {
    public static FolgaDTO from(Folga folga)
    {
        return new FolgaDTO(
                folga.getData(),
                folga.getMotivo()
        );
    }
}
