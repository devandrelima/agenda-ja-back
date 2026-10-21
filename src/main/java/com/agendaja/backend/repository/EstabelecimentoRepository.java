package com.agendaja.backend.repository;

import com.agendaja.backend.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {
	Optional<Estabelecimento> findByPublicId(UUID publicId);
}