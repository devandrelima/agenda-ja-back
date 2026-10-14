package com.agendaja.backend.dto;

import com.agendaja.backend.model.Profissional;
import java.util.List;

public record DisponibilidadeResponseDTO(
        Long profissionalId,
        List<JornadaTrabalhoDTO> jornadas,
        List<FolgaDTO> folgas
) {
    public static DisponibilidadeResponseDTO from(Profissional profissional)
    {
        return new DisponibilidadeResponseDTO(
                profissional.getId(),
                profissional.getJornadasTrabalho().stream().map(JornadaTrabalhoDTO::from).toList(),
                profissional.getFolgas().stream().map(FolgaDTO::from).toList()
        );
    }
}
