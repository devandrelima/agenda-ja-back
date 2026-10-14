package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.dto.ProfissionalResponseDTO;
import com.agendaja.backend.service.ProfissionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {
    private final ProfissionalService profissionalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfissionalResponseDTO criar(
            @PathVariable Long estabelecimentoId,
            @RequestBody @Valid ProfissionalRequestDTO dto,
            Authentication authentication
    ) {
        return profissionalService.criar(estabelecimentoId, dto, authentication.getName());
    }

    @GetMapping
    public List<ProfissionalResponseDTO> listar(
            @PathVariable Long estabelecimentoId,
            Authentication authentication
    ) {
        return profissionalService.listar(estabelecimentoId, authentication.getName());
    }

    @GetMapping("/{profissionalId}")
    public ProfissionalResponseDTO consultar(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long profissionalId,
            Authentication authentication
    ) {
        return profissionalService.consultar(estabelecimentoId, profissionalId, authentication.getName());
    }

    @PutMapping("/{profissionalId}")
    public ProfissionalResponseDTO editar(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long profissionalId,
            @RequestBody @Valid ProfissionalRequestDTO dto,
            Authentication authentication
    ) {
        return profissionalService.editar(estabelecimentoId, profissionalId, dto, authentication.getName());
    }
}
