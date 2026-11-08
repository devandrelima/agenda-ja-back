package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AgendamentoAgendaDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.service.AgendamentoGestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/agenda")
@RequiredArgsConstructor
@Tag(name = "Admin - Agenda", description = "Consulta administrativa e sincronização do calendário")
public class AdminAgendaController {
    private final AgendamentoGestorService agendamentoGestorService;

    @Operation(
            summary = "Consultar agenda administrativa",
            description = """
            Retorna os agendamentos de um determinado período (ex: semana atual).
            Suporta filtros opcionais por profissional e lista de status.
            O parâmetro opcional 'atualizadoApos' permite o Long Polling (Sincronização Incremental),
            retornando apenas os eventos criados ou alterados desde a última consulta do cliente.
            """
    )
    @GetMapping
    public List<AgendamentoAgendaDTO> consultarAgenda(
            @PathVariable Long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) List<StatusAgendamento> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime atualizadoApos,
            Authentication authentication
    ) {
        return agendamentoGestorService.listarAgenda(
                estabelecimentoId,
                inicio,
                fim,
                profissionalId,
                status,
                atualizadoApos,
                authentication.getName()
        );
    }
}
