
package com.agendaja.backend.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
@Getter
@Setter
@NoArgsConstructor
public class Servico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(name = "duracao_minutos", nullable = false)
	private Integer duracaoMinutos;

	@Column(
			nullable = false,
			precision = 10,
			scale = 2
	)
	private BigDecimal valor;

	@Column(nullable = false)
	private boolean ativo = true;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "estabelecimento_id",
			nullable = false
	)
	private Estabelecimento estabelecimento;
}
