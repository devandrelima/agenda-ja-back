
package com.agendaja.backend.dto;

import com.agendaja.backend.model.Servico;
import java.math.BigDecimal;

public record ServicoResponseDTO(
		Long id,
		String nome,
		Integer duracaoMinutos,
		BigDecimal valor,
		Long estabelecimentoId
) {
	public static ServicoResponseDTO from(Servico servico) {
		return new ServicoResponseDTO(
				servico.getId(),
				servico.getNome(),
				servico.getDuracaoMinutos(),
				servico.getValor(),
				servico.getEstabelecimento().getId()
		);
	}
}
