package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "estabelecimento_id")
    private Estabelecimento estabelecimento;

    private String nome;

    @Column(unique = true)
    private String email;

    private String senha;

    private String role;
}