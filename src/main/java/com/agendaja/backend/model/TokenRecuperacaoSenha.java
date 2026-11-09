package com.agendaja.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tokens_recuperacao_senha")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacaoSenha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(
			optional = false,
			fetch = FetchType.LAZY
	)
	@JoinColumn(
			name = "usuario_id",
			nullable = false
	)
	private Usuario usuario;

	@Column(
			name = "token_hash",
			nullable = false,
			unique = true,
			length = 64
	)
	private String tokenHash;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "expira_em", nullable = false)
	private Instant expiraEm;

	@Column(name = "utilizado_em")
	private Instant utilizadoEm;
}
