package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AlterarStatusRequestDTO;
import com.agendaja.backend.service.AgendamentoGestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/agendamentos/{agendamentoId}")
@RequiredArgsConstructor
@Tag(name = "Admin - Agendamentos", description = "Gerenciamento de agendamentos pelo gestor")
public class AdminAgendamentoController {
    private final AgendamentoGestorService agendamentoGestorService;

    @Operation(
            summary = "Alterar status do agendamento",
            description = "Altera o status de um agendamento (Confirmar, Concluir, Cancelar). Protegido contra transições inválidas."
    )
    @PatchMapping("/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarStatus(@PathVariable Long estabelecimentoId, @PathVariable Long agendamentoId, @RequestBody @Valid AlterarStatusRequestDTO dto, Authentication authentication
    )
    {
        agendamentoGestorService.alterarStatus(estabelecimentoId, agendamentoId, dto.status(), authentication.getName());
    }

    @Operation(
            summary = "Reagendar atendimento",
            description = "Altera a data e hora do agendamento. Valida colisões de horário no banco de dados para evitar double booking."
    )
    @PatchMapping("/horario")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reagendar(@PathVariable Long estabelecimentoId, @PathVariable Long agendamentoId, @RequestBody @Valid com.agendaja.backend.dto.ReagendamentoRequestDTO dto, Authentication authentication
    )
    {
        agendamentoGestorService.reagendar(estabelecimentoId, agendamentoId, dto.dataHoraInicio(), authentication.getName());
    }
}
