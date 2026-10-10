package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estabelecimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String categoria;

    @Column(name = "imagem_url")
    private String imagemUrl;

    private String localizacao;
}