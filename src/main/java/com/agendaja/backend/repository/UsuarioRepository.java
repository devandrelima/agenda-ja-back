package com.agendaja.backend.repository;

import com.agendaja.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTelefone(String telefone);
}
