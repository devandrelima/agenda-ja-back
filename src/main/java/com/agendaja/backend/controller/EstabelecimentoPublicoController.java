package com.agendaja.backend.controller;

import com.agendaja.backend.dto.EstabelecimentoPublicoDTO;
import com.agendaja.backend.service.EstabelecimentoPublicoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(
		name = "Portal Público - Estabelecimentos",
		description = "Consulta pública das informações dos estabelecimentos"
)
@RestController
@RequestMapping("/api/estabelecimentos/publico")
@RequiredArgsConstructor
public class EstabelecimentoPublicoController {

	private final EstabelecimentoPublicoService service;

	@Operation(
			summary = "Consultar estabelecimento pelo identificador público",
			description = """
                    Retorna os dados públicos de um estabelecimento
                    utilizando seu identificador UUID.

                    Endpoint público: não exige autenticação nem CSRF.

                    O id numérico retornado pode ser utilizado nos demais
                    endpoints públicos de serviços e profissionais.
                    """
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Estabelecimento encontrado"
			),
			@ApiResponse(
					responseCode = "400",
					description = "Identificador público inválido"
			),
			@ApiResponse(
					responseCode = "404",
					description = "Estabelecimento não encontrado"
			)
	})
	@GetMapping("/{publicId}")
	public EstabelecimentoPublicoDTO consultar(
			@PathVariable UUID publicId
	) {
		return service.consultar(publicId);
	}
}
