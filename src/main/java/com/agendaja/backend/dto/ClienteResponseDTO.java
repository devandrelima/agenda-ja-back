package com.agendaja.backend.dto;

import com.agendaja.backend.model.Usuario;

public record ClienteResponseDTO(
		Long id,
		String nome,
		String telefone,
		String email
) {
	public static ClienteResponseDTO from(Usuario cliente) {
		return new ClienteResponseDTO(
				cliente.getId(),
				cliente.getNome(),
				cliente.getTelefone(),
				cliente.getEmail()
		);
	}
}
