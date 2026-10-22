package com.agendaja.backend.controller;

import com.agendaja.backend.service.CalculadoraDisponibilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Portal Público - Horários", description = "Endpoints públicos para consultar a disponibilidade da agenda")
@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/servicos/{servicoId}/disponibilidade")
@RequiredArgsConstructor
public class PublicoDisponibilidadeController {
    private final CalculadoraDisponibilidadeService disponibilidadeService;

    @Operation(
            summary = "Consultar horários disponíveis",
            description = "Retorna horários livres no formato HH:mm."
    )
    @GetMapping
    public List<String> consultar(@PathVariable Long estabelecimentoId, @PathVariable Long servicoId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data, @RequestParam(required = false) Long profissionalId
    )
    {
        return disponibilidadeService.consultarHorarios(estabelecimentoId, servicoId, profissionalId, data);
    }
}
