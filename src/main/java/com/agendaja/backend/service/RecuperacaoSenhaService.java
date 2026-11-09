package com.agendaja.backend.service;

import com.agendaja.backend.dto.RedefinicaoSenhaDTO;
import com.agendaja.backend.model.TokenRecuperacaoSenha;
import com.agendaja.backend.repository.TokenRecuperacaoSenhaRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

	private static final Duration VALIDADE =
			Duration.ofMinutes(30);

	private static final Duration INTERVALO_SOLICITACOES =
			Duration.ofMinutes(1);

	private static final SecureRandom RANDOM =
			new SecureRandom();

	private final UsuarioRepository usuarioRepository;
	private final TokenRecuperacaoSenhaRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventos;

	@Value("${app.frontend.password-reset-url}")
	private String urlRedefinicao;

	@Transactional
	public void solicitar(String email) {
		String normalizado = email.strip()
				.toLowerCase(Locale.ROOT);

		var usuarioOpt = usuarioRepository
				.findByEmail(normalizado);

		// Não revelar se o e-mail pertence a uma conta.
		if (usuarioOpt.isEmpty()
				|| !usuarioOpt.get().isAtivo()) {
			return;
		}

		var usuario = usuarioOpt.get();
		Instant agora = Instant.now();

		// Evitar solicitações sucessivas para a mesma conta.
		var ultimoToken = tokenRepository
				.findTopByUsuarioIdOrderByCriadoEmDesc(
						usuario.getId()
				);

		if (ultimoToken.isPresent()
				&& ultimoToken.get().getCriadoEm()
				.isAfter(
						agora.minus(INTERVALO_SOLICITACOES)
				)) {
			return;
		}

		// Invalidar links anteriores.
		tokenRepository.invalidarTodosTokens(
				usuario.getId(),
				agora
		);

		// Gerar 256 bits aleatórios.
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);

		String token = Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(bytes);

		// Persistir somente o hash do token.
		TokenRecuperacaoSenha registro =
				new TokenRecuperacaoSenha();

		registro.setUsuario(usuario);
		registro.setTokenHash(hash(token));
		registro.setCriadoEm(agora);
		registro.setExpiraEm(agora.plus(VALIDADE));

		tokenRepository.save(registro);

		// Nunca construir a URL a partir do Host da requisição.
		String separador =
				urlRedefinicao.contains("?") ? "&" : "?";

		String link = urlRedefinicao
				+ separador
				+ "token="
				+ token;

		eventos.publishEvent(
				new EmailRecuperacaoSenhaEvent(
						usuario.getEmail(),
						link
				)
		);
	}

	@Transactional
	public void redefinir(RedefinicaoSenhaDTO dto) {
		var registro = tokenRepository
				.buscarPorHashParaAtualizar(
						hash(dto.token())
				)
				.orElseThrow(
						RecuperacaoSenhaService::tokenInvalido
				);

		Instant agora = Instant.now();

		if (registro.getUtilizadoEm() != null
				|| !registro.getExpiraEm().isAfter(agora)
				|| !registro.getUsuario().isAtivo()) {
			throw tokenInvalido();
		}

		var usuario = registro.getUsuario();

		// Reutilizar o BCrypt já configurado no projeto.
		usuario.setSenha(
				passwordEncoder.encode(dto.novaSenha())
		);

		// Consumir o token utilizado.
		registro.setUtilizadoEm(agora);

		// Invalidar quaisquer outros links da mesma conta.
		tokenRepository.invalidarOutrosTokens(
				usuario.getId(),
				registro.getId(),
				agora
		);
	}

	private static ResponseStatusException tokenInvalido() {
		return new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Link de recuperação inválido, expirado ou já utilizado."
		);
	}

	private static String hash(String token) {
		try {
			byte[] bytes = MessageDigest
					.getInstance("SHA-256")
					.digest(
							token.getBytes(
									StandardCharsets.UTF_8
							)
					);

			return HexFormat.of().formatHex(bytes);

		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(
					"SHA-256 indisponível",
					ex
			);
		}
	}
}
