package com.agendaja.backend.controller;

import com.agendaja.backend.dto.DashboardResumoDTO;
import com.agendaja.backend.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/dashboard")
@RequiredArgsConstructor
@Tag(
		name = "Admin - Dashboard",
		description = "Indicadores operacionais do estabelecimento"
)
public class DashboardController {

	private final DashboardService dashboardService;

	@Operation(
			summary = "Consultar dashboard operacional",
			description = """
                    Retorna os indicadores operacionais de um estabelecimento
                    para uma data específica. Caso a data não seja informada,
                    considera o dia atual.
                    """
	)
	@GetMapping
	public DashboardResumoDTO consultarDashboard(
			@PathVariable Long estabelecimentoId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate data,
			Authentication authentication
	) {
		return dashboardService.consultarDashboard(
				estabelecimentoId,
				data,
				authentication.getName()
		);
	}
}