package com.agendaja.backend.event;

import com.agendaja.backend.model.Agendamento;
import java.time.LocalDateTime;

public record AgendamentoReagendadoEvent(
    Agendamento agendamento,
    LocalDateTime dataHoraInicioAnterior,
    LocalDateTime dataHoraFimAnterior
) {}
