package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "estabelecimentos")
@Getter
@Setter
@NoArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;

    private String nome;

    private String categoria;

    @Column(name = "imagem_url", columnDefinition = "text")
    private String imagemUrl;

    @Column(name = "horarios_funcionamento", columnDefinition = "text")
    private String horariosFuncionamento;

    private String localizacao;

    public Estabelecimento(
            Long id,
            String nome,
            String categoria,
            String imagemUrl,
            String localizacao
    ) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.imagemUrl = imagemUrl;
        this.localizacao = localizacao;
    }

    @PrePersist
    public void gerarPublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
