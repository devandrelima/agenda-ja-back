package com.agendaja.backend.repository;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository
        extends JpaRepository<Agendamento, Long> {

    @EntityGraph(attributePaths = {
            "servico",
            "profissional",
            "profissional.estabelecimento"
    })
    List<Agendamento> findByClienteIdOrderByDataHoraInicioAsc(
            Long clienteId
    );

    @EntityGraph(attributePaths = {
            "servico",
            "profissional",
            "profissional.estabelecimento"
    })
    Optional<Agendamento> findByIdAndClienteId(
            Long id,
            Long clienteId
    );

    // Atualização condicional para evitar confirmação concorrente.
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        UPDATE Agendamento a
        SET a.status = :confirmado
        WHERE a.id = :agendamentoId
          AND a.cliente.id = :clienteId
          AND a.status = :pendente
    """)
    int confirmarSePendente(
            @Param("agendamentoId") Long agendamentoId,
            @Param("clienteId") Long clienteId,
            @Param("pendente") StatusAgendamento pendente,
            @Param("confirmado") StatusAgendamento confirmado
    );

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

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Agendamento a
        SET a.status = :cancelado
        WHERE a.id = :agendamentoId
          AND a.cliente.id = :clienteId
          AND a.status IN :statusPermitidos
    """)
    int cancelarSePermitido(
            @Param("agendamentoId") Long agendamentoId,
            @Param("clienteId") Long clienteId,
            @Param("statusPermitidos") List<StatusAgendamento> statusPermitidos,
            @Param("cancelado") StatusAgendamento cancelado
    );

}
