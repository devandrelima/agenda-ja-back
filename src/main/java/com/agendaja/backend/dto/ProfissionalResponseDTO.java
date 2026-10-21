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
                
                // especialidades enquanto não são retiradas do front
                profissional.getServicos().stream().map(servico -> servico.getNome()).toList(),
                
                profissional.getServicos().stream().map(servico -> servico.getId()).toList(),
                profissional.getJornadasTrabalho().stream().map(j -> j.getDiaSemana()).distinct().toList(),
                profissional.getFolgas().stream().map(f -> f.getData().toString()).toList(),
                profissional.getEstabelecimento().getId()
        );
    }
}
