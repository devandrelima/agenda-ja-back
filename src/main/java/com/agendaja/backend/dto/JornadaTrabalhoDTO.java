package com.agendaja.backend.dto;

import com.agendaja.backend.model.JornadaTrabalho;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record JornadaTrabalhoDTO(
        @NotNull(message = "O dia da semana é obrigatório")
        @Min(value = 1, message = "Dia da semana deve ser entre 1 e 7")
        @Max(value = 7, message = "Dia da semana deve ser entre 1 e 7")
        Integer diaSemana,

        @NotNull(message = "A hora de início é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "A hora de fim é obrigatória")
        LocalTime horaFim
) {
    public static JornadaTrabalhoDTO from(JornadaTrabalho jornada)
    {
        return new JornadaTrabalhoDTO(
                jornada.getDiaSemana(),
                jornada.getHoraInicio(),
                jornada.getHoraFim()
        );
    }
}
