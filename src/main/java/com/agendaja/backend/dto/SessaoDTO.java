
package com.agendaja.backend.dto;

public record SessaoDTO(
		Long id,
		String nome,
		String email,
		String role,
		Long estabelecimentoId
) {}
