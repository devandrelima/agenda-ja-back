package com.agendaja.backend.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstabelecimentoTest {
    @Test
    void deveGerarPublicIdAutomaticamenteSeNuloNoPrePersist() {
        Estabelecimento estabelecimento = new Estabelecimento();
        assertNull(estabelecimento.getPublicId());

        estabelecimento.gerarPublicId();

        assertNotNull(estabelecimento.getPublicId());
    }

    @Test
    void naoDeveSobrescreverPublicIdSeJaPreenchidoNoPrePersist() {
        Estabelecimento estabelecimento = new Estabelecimento();
        UUID uuidFixo = UUID.randomUUID();
        estabelecimento.setPublicId(uuidFixo);

        estabelecimento.gerarPublicId();

        assertEquals(uuidFixo, estabelecimento.getPublicId());
    }

    @Test
    void deveTestarConstrutorParametrizado() {
        Estabelecimento est = new Estabelecimento(1L, "Salão", "SALAO", "capa.jpg", "Rua X");

        assertEquals(1L, est.getId());
        assertEquals("Salão", est.getNome());
        assertEquals("SALAO", est.getCategoria());
        assertEquals("capa.jpg", est.getImagemUrl());
        assertEquals("Rua X", est.getLocalizacao());
    }
}
