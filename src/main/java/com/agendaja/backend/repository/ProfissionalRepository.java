package com.agendaja.backend.repository;

import com.agendaja.backend.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    List<Profissional> findByEstabelecimentoIdAndAtivoTrue(Long estabelecimentoId);
    Optional<Profissional> findByIdAndEstabelecimentoIdAndAtivoTrue(Long id, Long estabelecimentoId);    
    List<Profissional> findByEstabelecimentoIdAndServicosIdAndAtivoTrue(Long estabelecimentoId, Long servicoId);
}
