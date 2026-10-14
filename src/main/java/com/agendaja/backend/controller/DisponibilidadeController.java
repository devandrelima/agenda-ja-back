package com.agendaja.backend.controller;

import com.agendaja.backend.dto.DisponibilidadeRequestDTO;
import com.agendaja.backend.dto.DisponibilidadeResponseDTO;
import com.agendaja.backend.service.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/profissionais/{profissionalId}/disponibilidade")
@RequiredArgsConstructor
public class DisponibilidadeController {
    private final DisponibilidadeService disponibilidadeService;

    @GetMapping
    public DisponibilidadeResponseDTO consultar(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long profissionalId,
            Authentication authentication
    ) {
        return disponibilidadeService.consultar(estabelecimentoId, profissionalId, authentication.getName());
    }

    @PutMapping
    public DisponibilidadeResponseDTO atualizar(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long profissionalId,
            @RequestBody @Valid DisponibilidadeRequestDTO dto,
            Authentication authentication
    ) {
        return disponibilidadeService.atualizar(estabelecimentoId, profissionalId, dto, authentication.getName());
    }
}
