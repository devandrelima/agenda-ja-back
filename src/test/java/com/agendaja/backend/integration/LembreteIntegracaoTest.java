package com.agendaja.backend.integration;

import com.agendaja.backend.adapter.NotificadorService;
import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.job.LembreteAgendamentoJob;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class LembreteIntegracaoTest {
    @Autowired private LembreteAgendamentoJob lembreteJob;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;

    // Substitui o adaptador real por um mock para não enviar e-mails de verdade durante o teste
    @MockitoBean private NotificadorService notificadorService;

    private Long idAmanhaConfirmado;
    private Long idAmanhaPendente;

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();

        var estabelecimento = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão Teste", "SALAO", null, "Rua D"));
        var cliente = usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente Y", "cliente.lembrete@teste.com", "hash", "CLIENTE"));
        
        var servico = new Servico();
        servico.setNome("Corte");
        servico.setDuracaoMinutos(30);
        servico.setValor(new BigDecimal("50.00"));
        servico.setAtivo(true);
        servico.setEstabelecimento(estabelecimento);
        servico = servicoRepository.saveAndFlush(servico);
        
        var prof = new Profissional();
        prof.setNome("Roberta");
        prof.setEstabelecimento(estabelecimento);
        prof = profissionalRepository.saveAndFlush(prof);

        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);
        LocalDate depoisDeAmanha = hoje.plusDays(2);

        // Agendamento para HOJE (Não deve notificar)
        criarAgendamento(cliente, prof, servico, hoje.atTime(14, 0), StatusAgendamento.CONFIRMADO, false);

        // Agendamento para AMANHÃ Confirmado (DEVE notificar)
        idAmanhaConfirmado = criarAgendamento(cliente, prof, servico, amanha.atTime(9, 0), StatusAgendamento.CONFIRMADO, false).getId();

        // Agendamento para AMANHÃ Pendente (DEVE notificar)
        idAmanhaPendente = criarAgendamento(cliente, prof, servico, amanha.atTime(10, 0), StatusAgendamento.AGUARDANDO_CONFIRMACAO, false).getId();

        // Agendamento para AMANHÃ Cancelado (Não deve notificar)
        criarAgendamento(cliente, prof, servico, amanha.atTime(11, 0), StatusAgendamento.CANCELADO, false);

        // Agendamento para AMANHÃ já notificado antes (Não deve notificar)
        criarAgendamento(cliente, prof, servico, amanha.atTime(15, 0), StatusAgendamento.CONFIRMADO, true);

        // Agendamento para DEPOIS DE AMANHÃ (Não deve notificar)
        criarAgendamento(cliente, prof, servico, depoisDeAmanha.atTime(14, 0), StatusAgendamento.CONFIRMADO, false);
    }

    private Agendamento criarAgendamento(Usuario cliente, Profissional prof, Servico servico, LocalDateTime inicio, StatusAgendamento status, boolean lembreteEnviado) {
        var a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(prof);
        a.setServico(servico);
        a.setDataHoraInicio(inicio);
        a.setDataHoraFim(inicio.plusMinutes(30));
        a.setStatus(status);
        a.setLembreteEnviado(lembreteEnviado);
        return agendamentoRepository.saveAndFlush(a);
    }

    @Test
    void deveEnviarLembretesApenasParaAgendamentosValidosDeAmanha() {
        lembreteJob.processarLembretesDeAmanha();

        // O mock deve ter sido chamado exatament 2 vezes
        verify(notificadorService, times(2)).enviarLembreteAgendamento(any(Agendamento.class));

        // Verifica se a flag foi atualizada no banco
        var agendamentoConfirmado = agendamentoRepository.findById(idAmanhaConfirmado).orElseThrow();
        var agendamentoPendente = agendamentoRepository.findById(idAmanhaPendente).orElseThrow();
        
        assertTrue(agendamentoConfirmado.isLembreteEnviado());
        assertTrue(agendamentoPendente.isLembreteEnviado());
    }

    @Test
    void naoDeveEnviarLembreteDuplicadoSeOJobRodarDuasVezes() {
        // Primeira execução
        lembreteJob.processarLembretesDeAmanha();
        verify(notificadorService, times(2)).enviarLembreteAgendamento(any(Agendamento.class));

        // Segunda execução (simulando um disparo acidental ou reinício do servidor)
        lembreteJob.processarLembretesDeAmanha();
        
        // O mock CONTINUA tendo sido chamado apenas 2 vezes no total
        verify(notificadorService, times(2)).enviarLembreteAgendamento(any(Agendamento.class));
    }
}
