package com.agendaja.backend.dto;

import com.agendaja.backend.model.Profissional;
import java.util.List;

public record ProfissionalResponseDTO(
        Long id,
        String nome,
        List<String> especialidades,
        List<Long> idsServicos,
        List<Integer> diasDeAtendimento,
        List<String> diasDeFolga,
        Long estabelecimentoId
) {
    public static ProfissionalResponseDTO from(Profissional profissional)
    {
        return new ProfissionalResponseDTO(
                profissional.getId(),
                profissional.getNome(),
                profissional.getEspecialidades(),
                profissional.getServicos().stream().map(servico -> servico.getId()).toList(),
                profissional.getDiasDeAtendimento(),
                profissional.getDiasDeFolga(),
                profissional.getEstabelecimento().getId()
        );
    }
}
