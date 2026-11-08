package com.agendaja.backend.service;

import com.agendaja.backend.dto.ClienteAtualizacaoDTO;
import com.agendaja.backend.dto.ClienteCadastroDTO;
import com.agendaja.backend.dto.ClienteResponseDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {
	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public ClienteResponseDTO cadastrar(ClienteCadastroDTO dto)
	{
		if (usuarioRepository.existsByEmail(dto.email()))
			throw new EmailJaCadastradoException("E-mail já cadastrado no sistema.");

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

	@Transactional(readOnly = true)
    public ClienteResponseDTO consultarPerfil(String emailAutenticado)
	{
        Usuario cliente = buscarClientePorEmail(emailAutenticado);
        return ClienteResponseDTO.from(cliente);
    }

    @Transactional
    public ClienteResponseDTO atualizarPerfil(ClienteAtualizacaoDTO dto, String emailAutenticado)
	{
        Usuario cliente = buscarClientePorEmail(emailAutenticado);

        // Se o email foi alterado, valida se não pertence a outro usuário
        if (!cliente.getEmail().equals(dto.email())) {
            if (usuarioRepository.existsByEmail(dto.email()))
                throw new EmailJaCadastradoException("Este e-mail já está sendo utilizado por outra conta.");
            cliente.setEmail(dto.email());
        }

        cliente.setNome(dto.nome());
        cliente.setTelefone(dto.telefone());

        // A transação JPA cuidará de sincronizar a entidade com o banco via Dirty Checking (flush)
        return ClienteResponseDTO.from(cliente);
    }

    private Usuario buscarClientePorEmail(String email)
	{
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        if (!"CLIENTE".equals(usuario.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem gerenciar seu próprio perfil.");

        return usuario;
    }
}
