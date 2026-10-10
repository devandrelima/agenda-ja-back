package com.agendaja.backend.service;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void cadastrarEstabelecimentoEGestor(EstabelecimentoCadastroDTO dto) {

        String emailNormalizado = dto.email()
                .strip()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new EmailJaCadastradoException("E-mail já cadastrado no sistema.");
        }

        var estabelecimento = new Estabelecimento(null, dto.nomeNegocio(), dto.categoria(), dto.imagemUrl(), dto.localizacao());
        estabelecimento = estabelecimentoRepository.save(estabelecimento);

        var senhaHasheada = passwordEncoder.encode(dto.senha());
        var gestor = new Usuario(null, estabelecimento, dto.nomeGestor(), emailNormalizado, senhaHasheada, "GESTOR");

        usuarioRepository.save(gestor);
    }
}