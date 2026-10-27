package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(length = 20)
    private String telefone;

    public Usuario(
            Long id,
            Estabelecimento estabelecimento,
            String nome,
            String email,
            String senha,
            String role
    ) {
        this.id = id;
        this.estabelecimento = estabelecimento;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.role = role;
    }
}
