package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Entity
@Table(name = "jornadas_trabalho")
@Getter
@Setter
@NoArgsConstructor
public class JornadaTrabalho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;
}
