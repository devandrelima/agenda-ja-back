package com.agendaja.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AgendamentoTest {
    @Test
    void devePreencherCriadoEmSeNuloNoPrePersist() {
        Agendamento agendamento = new Agendamento();
        invokePrePersist(agendamento);

        assertNotNull(agendamento.getCriadoEm());
        assertNotNull(agendamento.getAtualizadoEm());
    }

    @Test
    void naoDeveSobrescreverCriadoEmSeJaPreenchidoNoPrePersist() {
        Agendamento agendamento = new Agendamento();
        LocalDateTime dataAntiga = LocalDateTime.of(2025, 5, 10, 8, 0);
        agendamento.setCriadoEm(dataAntiga);

        invokePrePersist(agendamento);

        assertEquals(dataAntiga, agendamento.getCriadoEm());
    }

    @Test
    void deveAtualizarAtualizadoEmNoPreUpdate() {
        Agendamento agendamento = new Agendamento();
        invokePreUpdate(agendamento);
        assertNotNull(agendamento.getAtualizadoEm());
    }

    private void invokePrePersist(Agendamento agendamento)
    {
        try {
            var method = Agendamento.class.getDeclaredMethod("antesDeInserir");
            method.setAccessible(true);
            method.invoke(agendamento);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokePreUpdate(Agendamento agendamento)
    {
        try {
            var method = Agendamento.class.getDeclaredMethod("antesDeAtualizar");
            method.setAccessible(true);
            method.invoke(agendamento);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
