package com.agendaja.backend.repository;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.id = :profissionalId
          AND a.status != :statusCancelado
          AND a.dataHoraInicio >= :inicioDia
          AND a.dataHoraInicio < :fimDia
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarAgendamentosAtivosDoDia(
            @Param("profissionalId") Long profissionalId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("fimDia") LocalDateTime fimDia,
            @Param("statusCancelado") StatusAgendamento statusCancelado
    );
}
