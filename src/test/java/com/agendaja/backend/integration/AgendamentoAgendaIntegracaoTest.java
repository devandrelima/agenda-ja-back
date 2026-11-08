package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.AgendamentoGestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AgendamentoAgendaIntegracaoTest {
    @Autowired private AgendamentoGestorService gestorService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;

    private Long estabelecimentoId;
    private Long profissional1Id;
    private final String emailGestor = "gestor.calendario@teste.com";

    private Long idAgendamentoHojeProg1;
    private Long idAgendamentoHojeProg2;
    private Long idAgendamentoAmanha;

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();

        var estabelecimento = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão HU14", "SALAO", null, "Rua C"));
        estabelecimentoId = estabelecimento.getId();

        usuarioRepository.saveAndFlush(new Usuario(null, estabelecimento, "Gestor", emailGestor, "hash", "GESTOR"));
        var cliente = usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente X", "cliente.hu14@teste.com", "hash", "CLIENTE"));

        var servico = new Servico();
        servico.setNome("Corte");
        servico.setDuracaoMinutos(30);
        servico.setValor(new BigDecimal("50.00"));
        servico.setAtivo(true);
        servico.setEstabelecimento(estabelecimento);
        servico = servicoRepository.saveAndFlush(servico);

        var prof1 = new Profissional();
        prof1.setNome("Marcos");
        prof1.setEstabelecimento(estabelecimento);
        prof1.setServicos(List.of(servico));
        prof1 = profissionalRepository.saveAndFlush(prof1);
        profissional1Id = prof1.getId();

        var prof2 = new Profissional();
        prof2.setNome("Sofia");
        prof2.setEstabelecimento(estabelecimento);
        prof2.setServicos(List.of(servico));
        prof2 = profissionalRepository.saveAndFlush(prof2);
        prof2.getId();

        // Agendamento Ontem (Profissional 1)
        var agendamento1 = criarAgendamento(cliente, prof1, servico, LocalDateTime.now().minusDays(1));
        agendamento1.getId();

        // Agendamento Hoje (Profissional 1)
        var agendamento2 = criarAgendamento(cliente, prof1, servico, LocalDateTime.now());
        idAgendamentoHojeProg1 = agendamento2.getId();

        // Agendamento Hoje (Profissional 2)
        var agendamento3 = criarAgendamento(cliente, prof2, servico, LocalDateTime.now().plusHours(1));
        idAgendamentoHojeProg2 = agendamento3.getId();

        // Agendamento Amanhã (Profissional 1)
        var agendamento4 = criarAgendamento(cliente, prof1, servico, LocalDateTime.now().plusDays(1));
        idAgendamentoAmanha = agendamento4.getId();
    }

    private Agendamento criarAgendamento(Usuario cliente, Profissional prof, Servico servico, LocalDateTime inicio) {
        var a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(prof);
        a.setServico(servico);
        a.setDataHoraInicio(inicio);
        a.setDataHoraFim(inicio.plusMinutes(30));
        a.setStatus(StatusAgendamento.CONFIRMADO);
        return agendamentoRepository.saveAndFlush(a);
    }

    @Test
    void deveFiltrarAgendamentosPorPeriodo() {
        // Busca apenas os agendamentos de HOJE
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);

        var resultados = gestorService.listarAgenda(
                estabelecimentoId, inicioHoje, fimHoje, null, null, null, emailGestor);

        assertEquals(2, resultados.size()); // Apenas os 2 de hoje
        assertTrue(resultados.stream().anyMatch(dto -> dto.id().equals(idAgendamentoHojeProg1)));
        assertTrue(resultados.stream().anyMatch(dto -> dto.id().equals(idAgendamentoHojeProg2)));
    }

    @Test
    void deveFiltrarAgendamentosPorProfissional() {
        // Busca apenas os agendamentos de HOJE, mas APENAS do Profissional 1
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);

        var resultados = gestorService.listarAgenda(
                estabelecimentoId, inicioHoje, fimHoje, profissional1Id, null, null, emailGestor);

        assertEquals(1, resultados.size());
        assertEquals(idAgendamentoHojeProg1, resultados.get(0).id());
    }

    @Test
    void deveRetornarApenasAgendamentosAtualizadosParaSincronizacaoIncremental() throws InterruptedException {
        // Simula o tempo a passar (100 milissegundos) para garantir diferença no timestamp
        Thread.sleep(100);
        LocalDateTime momentoDoPolling = LocalDateTime.now();
        Thread.sleep(100);

        // O gestor cancela o agendamento de amanhã. Isso acionará o @PreUpdate e mudará o 'atualizadoEm'
        gestorService.alterarStatus(estabelecimentoId, idAgendamentoAmanha, StatusAgendamento.CANCELADO, emailGestor);

        // O Frontend faz o Polling enviando a data/hora da última vez que perguntou
        LocalDateTime inicioAmplo = LocalDateTime.now().minusDays(5);
        LocalDateTime fimAmplo = LocalDateTime.now().plusDays(5);

        var resultados = gestorService.listarAgenda(
                estabelecimentoId, inicioAmplo, fimAmplo, null, null, momentoDoPolling, emailGestor);

        // O backend deve retornar EXATAMENTE UM registro (o que acabou de ser cancelado)
        // Ignorando os outros 3 que não sofreram alterações desde o 'momentoDoPolling'
        assertEquals(1, resultados.size());
        assertEquals(idAgendamentoAmanha, resultados.get(0).id());
        assertEquals("CANCELADO", resultados.get(0).status());
    }
}
