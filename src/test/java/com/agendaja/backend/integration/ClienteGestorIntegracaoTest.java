package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.ClienteGestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ClienteGestorIntegracaoTest {
    @Autowired private ClienteGestorService clienteGestorService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;

    private Long estabelecimentoAId;
    private Long estabelecimentoBId;
    private Long clienteId;
    private final String emailGestorA = "gestora@teste.com";
    private final String emailGestorB = "gestorb@teste.com";

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();

        // --- Estabelecimento A (Salão) ---
        var estA = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão A", "SALAO", null, "Rua A"));
        estabelecimentoAId = estA.getId();
        usuarioRepository.saveAndFlush(new Usuario(null, estA, "Gestor A", emailGestorA, "hash", "GESTOR"));

        Servico servicoCorteA = new Servico();
        servicoCorteA.setNome("Corte");
        servicoCorteA.setDuracaoMinutos(30);
        servicoCorteA.setValor(new BigDecimal("50.00"));
        servicoCorteA.setAtivo(true);
        servicoCorteA.setEstabelecimento(estA);
        servicoCorteA = servicoRepository.saveAndFlush(servicoCorteA);

        Servico servicoBarbaA = new Servico();
        servicoBarbaA.setNome("Barba");
        servicoBarbaA.setDuracaoMinutos(30);
        servicoBarbaA.setValor(new BigDecimal("30.00"));
        servicoBarbaA.setAtivo(true);
        servicoBarbaA.setEstabelecimento(estA);
        servicoBarbaA = servicoRepository.saveAndFlush(servicoBarbaA);

        var profA = new Profissional();
        profA.setNome("Carlos");
        profA.setEstabelecimento(estA);
        profA = profissionalRepository.saveAndFlush(profA);

        // --- Estabelecimento B (Clínica) ---
        var estB = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Clínica B", "CLINICA", null, "Rua B"));
        estabelecimentoBId = estB.getId();
        usuarioRepository.saveAndFlush(new Usuario(null, estB, "Gestor B", emailGestorB, "hash", "GESTOR"));

        Servico servicoMassagemB = new Servico();
        servicoMassagemB.setNome("Massagem");
        servicoMassagemB.setDuracaoMinutos(60);
        servicoMassagemB.setValor(new BigDecimal("100.00"));
        servicoMassagemB.setAtivo(true);
        servicoMassagemB.setEstabelecimento(estB);
        servicoMassagemB = servicoRepository.saveAndFlush(servicoMassagemB);

        var profB = new Profissional();
        profB.setNome("Ana");
        profB.setEstabelecimento(estB);
        profB = profissionalRepository.saveAndFlush(profB);

        // --- Cliente (Frequenta ambos os negócios) ---
        var cliente = new Usuario(null, null, "Cliente Fiel", "cliente@teste.com", "hash", "CLIENTE");
        cliente = usuarioRepository.saveAndFlush(cliente);
        clienteId = cliente.getId();

        // --- Histórico em A (3 Cortes, 1 Barba) ---
        LocalDateTime base = LocalDateTime.now();
        criarAgendamento(cliente, profA, servicoCorteA, base.minusDays(30), StatusAgendamento.CONCLUIDO);
        criarAgendamento(cliente, profA, servicoBarbaA, base.minusDays(20), StatusAgendamento.CONCLUIDO);
        criarAgendamento(cliente, profA, servicoCorteA, base.minusDays(10), StatusAgendamento.CONCLUIDO);
        // O último tem que ser CONFIRMADO para simular algo pendente de atendimento, mas que conta no histórico
        criarAgendamento(cliente, profA, servicoCorteA, base.minusDays(1), StatusAgendamento.CONFIRMADO);

        // Cancelado não deve entrar na contagem de histórico!
        criarAgendamento(cliente, profA, servicoCorteA, base.minusDays(5), StatusAgendamento.CANCELADO);

        // --- Histórico em B (2 Massagens) ---
        criarAgendamento(cliente, profB, servicoMassagemB, base.minusDays(15), StatusAgendamento.CONCLUIDO);
        criarAgendamento(cliente, profB, servicoMassagemB, base.minusDays(2), StatusAgendamento.CONCLUIDO);
    }

    private void criarAgendamento(Usuario cliente, Profissional prof, Servico servico, LocalDateTime inicio, StatusAgendamento status) {
        var a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(prof);
        a.setServico(servico);
        a.setDataHoraInicio(inicio);
        a.setDataHoraFim(inicio.plusMinutes(servico.getDuracaoMinutos()));
        a.setStatus(status);
        agendamentoRepository.saveAndFlush(a);
    }

    @Test
    void deveListarClientesResumoIsoladosPorEstabelecimento() {
        // Gestor A pede a lista de clientes
        var listaA = clienteGestorService.listarClientes(estabelecimentoAId, emailGestorA);
        assertEquals(1, listaA.size());
        assertEquals(4, listaA.get(0).totalVisitas()); // 3 cortes + 1 barba, ignora o cancelado

        // Gestor B pede a lista de clientes
        var listaB = clienteGestorService.listarClientes(estabelecimentoBId, emailGestorB);
        assertEquals(1, listaB.size());
        assertEquals(2, listaB.get(0).totalVisitas()); // 2 massagens
    }

    @Test
    void deveConsultarHistoricoECalcularServicoFavoritoIsolado() {
        // Histórico em A
        var historicoA = clienteGestorService.consultarHistorico(estabelecimentoAId, clienteId, emailGestorA);
        assertEquals(4, historicoA.agendamentos().size());
        assertEquals("Corte", historicoA.servicoMaisFrequente());

        // Histórico em B
        var historicoB = clienteGestorService.consultarHistorico(estabelecimentoBId, clienteId, emailGestorB);
        assertEquals(2, historicoB.agendamentos().size());
        assertEquals("Massagem", historicoB.servicoMaisFrequente());
    }

    @Test
    void deveProtegerContraAcessoIndevidoAoHistoricoDeOutroEstabelecimento() {
        // Gestor A tenta aceder ao estabelecimento B, o que é um ataque ou fuga de dados
        var erro = assertThrows(AccessDeniedException.class, () -> 
            clienteGestorService.consultarHistorico(estabelecimentoBId, clienteId, emailGestorA)
        );
        assertEquals("Acesso negado.", erro.getMessage());
    }
}
