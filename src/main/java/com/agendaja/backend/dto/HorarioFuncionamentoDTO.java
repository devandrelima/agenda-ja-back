package com.agendaja.backend.dto;

import java.util.List;

public record HorarioFuncionamentoDTO(List<Turno> turnos) {
    public record Turno(String inicio, String fim) {}
}
