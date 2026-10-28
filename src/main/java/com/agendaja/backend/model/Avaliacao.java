package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
		name = "avaliacoes",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_avaliacoes_cliente_estabelecimento",
				columnNames = {"cliente_id", "estabelecimento_id"}
		)
)
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Usuario cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estabelecimento_id", nullable = false)
	private Estabelecimento estabelecimento;

	@Column(nullable = false)
	private Integer nota;

	@Column(nullable = false, length = 1000)
	private String comentario;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private LocalDateTime atualizadoEm;

	@PrePersist
	private void antesDeInserir() {
		LocalDateTime agora = LocalDateTime.now();

		if (criadoEm == null) {
			criadoEm = agora;
		}

		atualizadoEm = agora;
	}

	@PreUpdate
	private void antesDeAtualizar() {
		atualizadoEm = LocalDateTime.now();
	}
}
