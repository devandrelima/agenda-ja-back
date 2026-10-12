
package com.agendaja.backend.security;

import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	@Override
	public UserDetails loadUserByUsername(String email)
			throws UsernameNotFoundException {

		String emailNormalizado = email.strip()
				.toLowerCase(Locale.ROOT);

		var usuario = usuarioRepository
				.findByEmail(emailNormalizado)
				.orElseThrow(() ->
						new UsernameNotFoundException(
								"Usuário não encontrado"
						)
				);

		return User.builder()
				.username(usuario.getEmail())
				.password(usuario.getSenha())
				.roles(usuario.getRole())
				.build();
	}
}
