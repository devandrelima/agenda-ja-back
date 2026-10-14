package com.agendaja.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DisponibilidadeRequestDTO(
        @NotNull(message = "Jornadas não podem ser nulas")
        @Valid
        List<JornadaTrabalhoDTO> jornadas,

        @NotNull(message = "Folgas não podem ser nulas")
        @Valid
        List<FolgaDTO> folgas
) {}
