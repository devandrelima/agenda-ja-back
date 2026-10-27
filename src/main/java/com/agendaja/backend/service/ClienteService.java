package com.agendaja.backend.service;

import com.agendaja.backend.dto.ClienteCadastroDTO;
import com.agendaja.backend.dto.ClienteResponseDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public ClienteResponseDTO cadastrar(ClienteCadastroDTO dto) {

		if (usuarioRepository.existsByEmail(dto.email())) {
			throw new EmailJaCadastradoException(
					"E-mail já cadastrado no sistema."
			);
		}

		var cliente = new Usuario(
				null,
				null,
				dto.nome(),
				dto.email(),
				passwordEncoder.encode(dto.senha()),
				"CLIENTE"
		);

		cliente.setTelefone(dto.telefone());

		var clienteSalvo = usuarioRepository.save(cliente);

		return ClienteResponseDTO.from(clienteSalvo);
	}
}
