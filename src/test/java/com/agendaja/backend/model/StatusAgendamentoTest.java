package com.agendaja.backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusAgendamentoTest {
    @Test
    void transicoesAguardandoConfirmacao_devemEstarCorretas() {
        assertTrue(StatusAgendamento.AGUARDANDO_CONFIRMACAO.podeTransitarPara(StatusAgendamento.CONFIRMADO));
        assertTrue(StatusAgendamento.AGUARDANDO_CONFIRMACAO.podeTransitarPara(StatusAgendamento.CANCELADO));
        
        assertFalse(StatusAgendamento.AGUARDANDO_CONFIRMACAO.podeTransitarPara(StatusAgendamento.CONCLUIDO));
        assertFalse(StatusAgendamento.AGUARDANDO_CONFIRMACAO.podeTransitarPara(StatusAgendamento.AGUARDANDO_CONFIRMACAO));
    }

    @Test
    void transicoesConfirmado_devemEstarCorretas() {
        assertTrue(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.CONCLUIDO));
        assertTrue(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.CANCELADO));
        
        assertFalse(StatusAgendamento.CONFIRMADO.podeTransitarPara(StatusAgendamento.AGUARDANDO_CONFIRMACAO));
    }

    @Test
    void estadosFinais_naoDevemPossuirTransicoes() {
        assertFalse(StatusAgendamento.CANCELADO.podeTransitarPara(StatusAgendamento.CONFIRMADO));
        assertFalse(StatusAgendamento.CANCELADO.podeTransitarPara(StatusAgendamento.AGUARDANDO_CONFIRMACAO));
        
        assertFalse(StatusAgendamento.CONCLUIDO.podeTransitarPara(StatusAgendamento.AGUARDANDO_CONFIRMACAO));
        assertFalse(StatusAgendamento.CONCLUIDO.podeTransitarPara(StatusAgendamento.CANCELADO));
    }
}
