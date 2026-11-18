package com.agendaja.backend.dto;

import com.agendaja.backend.model.Estabelecimento;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstabelecimentoResumoPublicoDTOTest {
    private Estabelecimento criarEstabelecimentoCompleto() {
        var estabelecimento = new Estabelecimento(
                1L,
                "Studio Marina",
                "Salão de beleza",
                "/assets/capa.png",
                "Centro, Mossoró - RN"
        );
        estabelecimento.setPublicId(UUID.randomUUID());
        estabelecimento.setLogoUrl("/assets/logo.png");
        return estabelecimento;
    }

    @Test
    void deveMapearEstabelecimentoComAvaliacoes() {
        var estabelecimento = criarEstabelecimentoCompleto();

        var dto = EstabelecimentoResumoPublicoDTO.from(estabelecimento, 4.5, 10L);

        assertEquals(estabelecimento.getId(), dto.id());
        assertEquals(estabelecimento.getPublicId(), dto.publicId());
        assertEquals("Studio Marina", dto.nome());
        assertEquals("Salão de beleza", dto.categoria());
        assertEquals("Centro, Mossoró - RN", dto.localizacao());
        assertEquals("/assets/capa.png", dto.imagemUrl());
        assertEquals("/assets/logo.png", dto.logoUrl());
        assertEquals(4.5, ((Number) dto.mediaAvaliacoes()).doubleValue(), 0.001);
        assertEquals(10L, ((Number) dto.quantidadeAvaliacoes()).longValue());
    }

    @Test
    void deveRetornarMediaZeroQuandoMediaForNulaESemAvaliacoes() {
        var estabelecimento = new Estabelecimento(
                2L,
                "Barbearia Central",
                "Barbearia",
                null,
                "Centro"
        );

        var dto = EstabelecimentoResumoPublicoDTO.from(estabelecimento, null, 0L);

        assertEquals(2L, dto.id());
        assertNull(dto.imagemUrl());
        assertNull(dto.logoUrl());
        assertEquals(0.0, ((Number) dto.mediaAvaliacoes()).doubleValue(), 0.001);
        assertEquals(0L, ((Number) dto.quantidadeAvaliacoes()).longValue());
    }

    @Test
    void deveCobrirRamosComMediaNulaEQuantidadePositivaOuMediaPresenteEQuantidadeZero() {
        var estabelecimento = criarEstabelecimentoCompleto();

        var dtoMediaNula = EstabelecimentoResumoPublicoDTO.from(estabelecimento, null, 5L);
        assertEquals(0.0, ((Number) dtoMediaNula.mediaAvaliacoes()).doubleValue(), 0.001);
        assertEquals(5L, ((Number) dtoMediaNula.quantidadeAvaliacoes()).longValue());

        var dtoQuantidadeZero = EstabelecimentoResumoPublicoDTO.from(estabelecimento, 4.0, 0L);
        assertTrue(((Number) dtoQuantidadeZero.mediaAvaliacoes()).doubleValue() >= 0.0);
        assertEquals(0L, ((Number) dtoQuantidadeZero.quantidadeAvaliacoes()).longValue());
    }
}
