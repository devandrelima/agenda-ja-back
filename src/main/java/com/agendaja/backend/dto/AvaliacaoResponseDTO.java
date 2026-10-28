package com.agendaja.backend.dto;

import com.agendaja.backend.model.Avaliacao;

import java.time.LocalDateTime;

public record AvaliacaoResponseDTO(
		Long id,
		Long clienteId,
		String nomeCliente,
		Integer nota,
		String comentario,
		LocalDateTime criadoEm,
		LocalDateTime atualizadoEm
) {
	public static AvaliacaoResponseDTO from(Avaliacao avaliacao) {
		return new AvaliacaoResponseDTO(
				avaliacao.getId(),
				avaliacao.getCliente().getId(),
				avaliacao.getCliente().getNome(),
				avaliacao.getNota(),
				avaliacao.getComentario(),
				avaliacao.getCriadoEm(),
				avaliacao.getAtualizadoEm()
		);
	}
}
