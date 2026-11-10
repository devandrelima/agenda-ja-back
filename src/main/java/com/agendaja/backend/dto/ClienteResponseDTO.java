package com.agendaja.backend.dto;

import com.agendaja.backend.model.Usuario;

public record ClienteResponseDTO(
		Long id,
		String nome,
		String telefone,
		String email,
		String fotoUrl
) {
	public static ClienteResponseDTO from(Usuario cliente) {
		return new ClienteResponseDTO(
				cliente.getId(),
				cliente.getNome(),
				cliente.getTelefone(),
			cliente.getEmail(),
			cliente.getFotoUrl()
		);
	}
}
