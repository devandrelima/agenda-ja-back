package com.agendaja.backend.controller;

import com.agendaja.backend.dto.RelatorioOperacionalDTO;
import com.agendaja.backend.service.RelatorioOperacionalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/relatorios")
@RequiredArgsConstructor
@Tag(
		name = "Admin - Relatórios",
		description = "Relatórios operacionais do estabelecimento"
)
public class RelatorioOperacionalController {

	private final RelatorioOperacionalService relatorioOperacionalService;

	@Operation(
			summary = "Consultar relatório operacional",
			description = """
                    Retorna os atendimentos concluídos, faturamento dos concluídos,
                    taxa de confirmação e serviços mais populares no período informado.
                    """
	)
	@GetMapping
	public RelatorioOperacionalDTO consultarRelatorio(
			@PathVariable Long estabelecimentoId,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate dataInicio,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate dataFim,
			Authentication authentication
	) {
		return relatorioOperacionalService.consultarRelatorio(
				estabelecimentoId,
				dataInicio,
				dataFim,
				authentication.getName()
		);
	}
}
