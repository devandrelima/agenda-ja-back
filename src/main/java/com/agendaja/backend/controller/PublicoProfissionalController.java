package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ProfissionalResponseDTO;
import com.agendaja.backend.service.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Portal Público - Profissionais", description = "Endpoints públicos para clientes do estabelecimento")
@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/servicos/{servicoId}/profissionais")
@RequiredArgsConstructor
public class PublicoProfissionalController {
    private final ProfissionalService profissionalService;

    @Operation(
            summary = "Listar profissionais por serviço",
            description = "Retorna todos os profissionais de um estabelecimento que realizam um serviço específico. Endpoint público."
    )
    @GetMapping
    public List<ProfissionalResponseDTO> listarPorServico(@PathVariable Long estabelecimentoId, @PathVariable Long servicoId)
    {
        return profissionalService.listarPorServicoPublico(estabelecimentoId, servicoId);
    }

    @Operation(
            summary = "Sortear profissional elegível",
            description = "Retorna um profissional aleatório que realize o serviço especificado."
    )
    @GetMapping("/aleatorio")
    public ProfissionalResponseDTO sortearPorServico(@PathVariable Long estabelecimentoId, @PathVariable Long servicoId)
    {
        return profissionalService.sortearProfissionalPorServico(estabelecimentoId, servicoId);
    }
}
