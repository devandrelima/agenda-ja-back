
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
	public UserDetails loadUserByUsername(String identificador)
			throws UsernameNotFoundException {

		String entrada = identificador.strip();

		var usuario = (entrada.contains("@")
				? usuarioRepository.findByEmail(
						entrada.toLowerCase(Locale.ROOT)
				)
				: usuarioRepository.findByTelefone(
						entrada.replaceAll("\\D", "")
				))
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
