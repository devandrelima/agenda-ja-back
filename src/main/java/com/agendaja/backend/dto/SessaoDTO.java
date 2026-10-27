
package com.agendaja.backend.dto;

public record SessaoDTO(
		Long id,
		String nome,
		String email,
		String telefone,
		String role,
		Long estabelecimentoId
) {}
