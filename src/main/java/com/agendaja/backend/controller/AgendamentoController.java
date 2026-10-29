package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AgendamentoDetalhesDTO;
import com.agendaja.backend.dto.AgendamentoRequestDTO;
import com.agendaja.backend.dto.AgendamentoResponseDTO;
import com.agendaja.backend.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agendamentos", description = "Endpoints para registro e gerenciamento de agendamentos")
@RestController
@RequestMapping("/api/estabelecimentos/{estabelecimentoId}/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {
    private final AgendamentoService agendamentoService;

    @Operation(
            summary = "Registrar novo agendamento",
            description = "Efetiva a reserva no banco garantindo prevenção contra concorrência e sobreposição."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponseDTO registrar(@PathVariable Long estabelecimentoId, @RequestBody @Valid AgendamentoRequestDTO dto, Authentication authentication)
    {
        // O email vem do token de autenticação, garantindo que o cliente marque para si mesmo
        return agendamentoService.registrar(estabelecimentoId, dto, authentication.getName());
    }

    @Operation(
            summary = "Consultar detalhes do agendamento",
            description = "Retorna os dados completos da reserva. Acessível apenas para o cliente dono do agendamento ou para o gestor do estabelecimento."
    )
    @GetMapping("/{agendamentoId}")
    public AgendamentoDetalhesDTO consultar(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long agendamentoId,
            Authentication authentication
    ) {
        return agendamentoService.consultarDetalhes(estabelecimentoId, agendamentoId, authentication.getName());
    }
}
