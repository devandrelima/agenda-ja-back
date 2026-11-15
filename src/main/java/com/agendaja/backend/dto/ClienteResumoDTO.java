package com.agendaja.backend.dto;

import com.agendaja.backend.repository.ClienteResumoProjection;
import java.time.LocalDateTime;

public record ClienteResumoDTO(
        Long id,
        String nome,
        String telefone,
        Integer totalVisitas,
        LocalDateTime ultimaVisita
) {
    public static ClienteResumoDTO from(ClienteResumoProjection projection)
    {
        return new ClienteResumoDTO(
                projection.getClienteId(),
                projection.getNome(),
                projection.getTelefone(),
                projection.getTotalVisitas(),
                projection.getUltimaVisita()
        );
    }
}
