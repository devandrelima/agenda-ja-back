
package com.agendaja.backend.security;

import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstabelecimentoAuthorizationService {

	private final UsuarioRepository usuarioRepository;

	public void validarAcesso(
			String email,
			Long estabelecimentoId
	) {
		var usuario = usuarioRepository.findByEmail(email)
				.orElseThrow(() ->
						new AccessDeniedException("Acesso negado.")
				);

		if (!"GESTOR".equals(usuario.getRole())
				|| usuario.getEstabelecimento() == null
				|| !estabelecimentoId.equals(
				usuario.getEstabelecimento().getId()
		)) {

			throw new AccessDeniedException("Acesso negado.");
		}
	}
}
