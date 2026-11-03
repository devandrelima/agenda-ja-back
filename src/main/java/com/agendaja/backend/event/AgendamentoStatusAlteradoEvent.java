package com.agendaja.backend.event;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;

public record AgendamentoStatusAlteradoEvent(
    Agendamento agendamento,
    StatusAgendamento statusAnterior
) {}
