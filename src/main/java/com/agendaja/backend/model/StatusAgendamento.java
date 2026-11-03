package com.agendaja.backend.model;

import java.util.Set;

public enum StatusAgendamento {
    AGUARDANDO_CONFIRMACAO {
        @Override
        public Set<StatusAgendamento> transicoesValidas() {
            // Pode ser aceite pelo gestor ou cancelado
            return Set.of(CONFIRMADO, CANCELADO);
        }
    },

    CONFIRMADO {
        @Override
        public Set<StatusAgendamento> transicoesValidas() {
            // Pode ser finalizado após o atendimento ou cancelado
            return Set.of(CONCLUIDO, CANCELADO);
        }
    },

    CANCELADO {
        @Override
        public Set<StatusAgendamento> transicoesValidas() {
            // Estado final
            return Set.of(); 
        }
    },

    CONCLUIDO {
        @Override
        public Set<StatusAgendamento> transicoesValidas() {
            // Estado final
            return Set.of(); 
        }
    };

    /**
     * Define quais são os próximos estados permitidos a partir do estado atual.
     */
    protected abstract Set<StatusAgendamento> transicoesValidas();

    /**
     * Valida se o agendamento pode mudar do seu estado atual para o novo estado solicitado.
     */
    public boolean podeTransitarPara(StatusAgendamento novoStatus)
    {
        return transicoesValidas().contains(novoStatus);
    }
}
