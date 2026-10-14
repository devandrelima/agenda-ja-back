package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profissionais")
@Getter
@Setter
@NoArgsConstructor
public class Profissional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @ManyToMany
    @JoinTable(
            name = "profissionais_servicos",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profissional_especialidades", joinColumns = @JoinColumn(name = "profissional_id"))
    @Column(name = "especialidade")
    private List<String> especialidades = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profissional_dias_atendimento", joinColumns = @JoinColumn(name = "profissional_id"))
    @Column(name = "dia_semana")
    private List<Integer> diasDeAtendimento = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profissional_dias_folga", joinColumns = @JoinColumn(name = "profissional_id"))
    @Column(name = "dia_folga")
    private List<String> diasDeFolga = new ArrayList<>();
}
