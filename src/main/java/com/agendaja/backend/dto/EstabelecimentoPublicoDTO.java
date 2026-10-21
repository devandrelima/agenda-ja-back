package com.agendaja.backend.dto;

import com.agendaja.backend.model.Estabelecimento;

import java.util.UUID;

public record EstabelecimentoPublicoDTO(
		Long id,
		UUID publicId,
		String nome,
		String categoria,
		String imagemUrl,
		String localizacao
) {
	public static EstabelecimentoPublicoDTO from(
			Estabelecimento estabelecimento
	) {
		return new EstabelecimentoPublicoDTO(
				estabelecimento.getId(),
				estabelecimento.getPublicId(),
				estabelecimento.getNome(),
				estabelecimento.getCategoria(),
				estabelecimento.getImagemUrl(),
				estabelecimento.getLocalizacao()
		);
	}
}
