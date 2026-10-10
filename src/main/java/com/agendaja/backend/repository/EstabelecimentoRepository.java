package com.agendaja.backend.repository;

import com.agendaja.backend.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {
}