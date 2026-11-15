package com.agendaja.backend.dto;

import java.util.List;

public record ClienteHistoricoDTO(
        Long id,
        String nome,
        String telefone,
        String servicoMaisFrequente,
        List<AgendamentoHistoricoDTO> agendamentos
) {}
