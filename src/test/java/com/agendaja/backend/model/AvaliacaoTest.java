package com.agendaja.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoTest {
    @Test
    void devePreencherCriadoEmSeNuloNoPrePersist() {
        Avaliacao avaliacao = new Avaliacao();
        assertNull(avaliacao.getCriadoEm());

        // Força a execução do callback JPA via reflexão ou simulação
        invokePrePersist(avaliacao);

        assertNotNull(avaliacao.getCriadoEm());
        assertNotNull(avaliacao.getAtualizadoEm());
    }

    @Test
    void naoDeveSobrescreverCriadoEmSeJaPreenchidoNoPrePersist() {
        Avaliacao avaliacao = new Avaliacao();
        LocalDateTime dataAntiga = LocalDateTime.of(2025, 1, 1, 10, 0);
        avaliacao.setCriadoEm(dataAntiga);

        invokePrePersist(avaliacao);

        assertEquals(dataAntiga, avaliacao.getCriadoEm());
    }

    @Test
    void deveAtualizarAtualizadoEmNoPreUpdate() {
        Avaliacao avaliacao = new Avaliacao();
        invokePreUpdate(avaliacao);
        assertNotNull(avaliacao.getAtualizadoEm());
    }

    private void invokePrePersist(Avaliacao avaliacao)
    {
        try {
            var method = Avaliacao.class.getDeclaredMethod("antesDeInserir");
            method.setAccessible(true);
            method.invoke(avaliacao);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokePreUpdate(Avaliacao avaliacao)
    {
        try {
            var method = Avaliacao.class.getDeclaredMethod("antesDeAtualizar");
            method.setAccessible(true);
            method.invoke(avaliacao);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
