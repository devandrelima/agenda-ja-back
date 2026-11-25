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

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
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

    @EntityGraph(attributePaths = {
            "cliente",
            "servico",
            "profissional"
    })
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.estabelecimento.id = :estabelecimentoId
          AND a.dataHoraInicio >= :inicio
          AND a.dataHoraInicio <= :fim
          AND (:profissionalId IS NULL OR a.profissional.id = :profissionalId)
          AND a.status IN :statusList
          AND (CAST(:atualizadoApos AS timestamp) IS NULL OR a.atualizadoEm > :atualizadoApos)
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarAgendaAdministrativa(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("profissionalId") Long profissionalId,
            @Param("statusList") List<StatusAgendamento> statusList,
            @Param("atualizadoApos") LocalDateTime atualizadoApos
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Agendamento a
        SET a.status = :cancelado
        WHERE a.cliente.id = :clienteId
          AND a.dataHoraInicio > :agora
          AND a.status IN :statusPermitidos
    """)
    int cancelarAgendamentosFuturosDoCliente(
            @Param("clienteId") Long clienteId,
            @Param("agora") LocalDateTime agora,
            @Param("statusPermitidos") List<StatusAgendamento> statusPermitidos,
            @Param("cancelado") StatusAgendamento cancelado
    );


    @EntityGraph(attributePaths = {
            "cliente",
            "servico",
            "profissional.estabelecimento"
    })
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.dataHoraInicio >= :inicioAmanha
          AND a.dataHoraInicio < :fimAmanha
          AND a.status IN :statusValidos
          AND a.lembreteEnviado = false
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarElegiveisParaLembrete(
            @Param("inicioAmanha") LocalDateTime inicioAmanha,
            @Param("fimAmanha") LocalDateTime fimAmanha,
            @Param("statusValidos") List<StatusAgendamento> statusValidos
    );


    @Query("""
        SELECT 
            c.id AS clienteId, 
            c.nome AS nome, 
            c.telefone AS telefone, 
            COUNT(a.id) AS totalVisitas, 
            MAX(a.dataHoraInicio) AS ultimaVisita
        FROM Agendamento a
        JOIN a.cliente c
        WHERE a.profissional.estabelecimento.id = :estabelecimentoId
          AND a.status IN ('CONFIRMADO', 'CONCLUIDO')
        GROUP BY c.id, c.nome, c.telefone
        ORDER BY MAX(a.dataHoraInicio) DESC
    """)
    List<ClienteResumoProjection> buscarResumoClientesPorEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId
    );
    

    @EntityGraph(attributePaths = {
            "servico",
            "profissional"
    })
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.estabelecimento.id = :estabelecimentoId
          AND a.cliente.id = :clienteId
          AND a.status IN ('CONFIRMADO', 'CONCLUIDO')
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> buscarHistoricoDoClienteNoEstabelecimento(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("clienteId") Long clienteId
    );

    @Query("""
        SELECT
            COUNT(a.id) AS totalReservas,
    
            COUNT(CASE
                WHEN a.status = :confirmado THEN a.id
            END) AS confirmados,
    
            COUNT(CASE
                WHEN a.status = :pendente THEN a.id
            END) AS pendentes,
    
            COUNT(CASE
                WHEN a.status = :cancelado THEN a.id
            END) AS cancelados,
    
            COUNT(CASE
                WHEN a.status = :concluido THEN a.id
            END) AS concluidos,
    
            COALESCE(SUM(CASE
                WHEN a.status <> :cancelado THEN a.servico.valor
                ELSE NULL
            END), 0) AS faturamentoPrevisto
    
        FROM Agendamento a
        WHERE a.profissional.estabelecimento.id = :estabelecimentoId
          AND a.dataHoraInicio >= :inicioDia
          AND a.dataHoraInicio < :fimDia
    """)
    DashboardAgendamentoProjection buscarResumoDashboard(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("fimDia") LocalDateTime fimDia,
            @Param("confirmado") StatusAgendamento confirmado,
            @Param("pendente") StatusAgendamento pendente,
            @Param("cancelado") StatusAgendamento cancelado,
            @Param("concluido") StatusAgendamento concluido
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "servico",
            "profissional"
    })
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.profissional.estabelecimento.id = :estabelecimentoId
          AND a.dataHoraInicio >= :agora
          AND a.dataHoraInicio < :fimDia
          AND a.status IN :statusList
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarProximosAtendimentos(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("agora") LocalDateTime agora,
            @Param("fimDia") LocalDateTime fimDia,
            @Param("statusList") List<StatusAgendamento> statusList
    );

    @Query("""
    SELECT
        COUNT(CASE
            WHEN a.status = :concluido THEN a.id
        END) AS totalConcluidos,

        COALESCE(SUM(CASE
            WHEN a.status = :concluido THEN a.servico.valor
            ELSE 0
        END), 0) AS faturamentoConcluidos,

        COUNT(CASE
            WHEN a.status = :confirmado
              OR a.status = :concluido
            THEN a.id
        END) AS totalConfirmados,

        COUNT(CASE
            WHEN a.status <> :cancelado
            THEN a.id
        END) AS totalConsideradosNaTaxa

    FROM Agendamento a
    WHERE a.profissional.estabelecimento.id = :estabelecimentoId
      AND a.dataHoraInicio >= :inicio
      AND a.dataHoraInicio < :fim
""")
    RelatorioOperacionalProjection buscarIndicadoresRelatorio(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("concluido") StatusAgendamento concluido,
            @Param("confirmado") StatusAgendamento confirmado,
            @Param("cancelado") StatusAgendamento cancelado
    );

    @Query("""
    SELECT
        s.id AS servicoId,
        s.nome AS servicoNome,
        COUNT(a.id) AS quantidadeAgendamentos

    FROM Agendamento a
    JOIN a.servico s

    WHERE a.profissional.estabelecimento.id = :estabelecimentoId
      AND a.dataHoraInicio >= :inicio
      AND a.dataHoraInicio < :fim
      AND a.status <> :cancelado

    GROUP BY s.id, s.nome
    ORDER BY COUNT(a.id) DESC, s.nome ASC
""")
    List<ServicoPopularProjection> buscarServicosMaisPopulares(
            @Param("estabelecimentoId") Long estabelecimentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelado") StatusAgendamento cancelado
    );

}
