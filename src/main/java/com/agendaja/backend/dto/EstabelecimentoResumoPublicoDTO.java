package com.agendaja.backend.dto;

import com.agendaja.backend.model.Estabelecimento;

import java.util.UUID;

public record EstabelecimentoResumoPublicoDTO(
        Long id,
        UUID publicId,
        String nome,
        String categoria,
        String localizacao,
        String imagemUrl,
        String logoUrl,
        double mediaAvaliacoes,
        long quantidadeAvaliacoes
) {
    public static EstabelecimentoResumoPublicoDTO from(
            Estabelecimento estabelecimento, Double mediaAvaliacoes, long quantidadeAvaliacoes) {
        return new EstabelecimentoResumoPublicoDTO(
                estabelecimento.getId(),
                estabelecimento.getPublicId(),
                estabelecimento.getNome(),
                estabelecimento.getCategoria(),
                estabelecimento.getLocalizacao(),
                estabelecimento.getImagemUrl(),
                estabelecimento.getLogoUrl(),
                mediaAvaliacoes == null ? 0 : mediaAvaliacoes,
                quantidadeAvaliacoes
        );
    }
}
