package com.agendaja.backend.dto;

import com.agendaja.backend.model.Estabelecimento;

import java.util.UUID;
import java.util.List;

public record EstabelecimentoPublicoDTO(
		Long id,
		UUID publicId,
		String nome,
		String categoria,
		String imagemUrl,
		String localizacao,
		List<HorarioFuncionamentoDTO> horariosFuncionamento
) {
	public static EstabelecimentoPublicoDTO from(
			Estabelecimento estabelecimento,
			List<HorarioFuncionamentoDTO> horarios
	) {
		return new EstabelecimentoPublicoDTO(
				estabelecimento.getId(),
				estabelecimento.getPublicId(),
				estabelecimento.getNome(),
				estabelecimento.getCategoria(),
				estabelecimento.getImagemUrl(),
				estabelecimento.getLocalizacao(),
				horarios
		);
	}
}
