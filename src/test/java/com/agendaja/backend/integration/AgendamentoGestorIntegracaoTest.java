package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.service.AgendamentoGestorService;
import com.agendaja.backend.service.CalculadoraDisponibilidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AgendamentoGestorIntegracaoTest {
    @Autowired private AgendamentoGestorService gestorService;
    @Autowired private CalculadoraDisponibilidadeService disponibilidadeService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;

    private Long estabelecimentoId;
    private Long agendamentoId;
    private Long profissionalId;
    private Long servicoId;
    private final String emailGestor = "gestor.agenda@teste.com";

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();
        
        var estabelecimento = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão HU16", "SALAO", null, "Rua B"));
        estabelecimentoId = estabelecimento.getId();

        usuarioRepository.saveAndFlush(new Usuario(null, estabelecimento, "Gestor HU16", emailGestor, "hash", "GESTOR"));
        var cliente = usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente", "cliente.hu16@teste.com", "hash", "CLIENTE"));

        Servico novoServico = new Servico();
        novoServico.setNome("Corte");
        novoServico.setDuracaoMinutos(30);
        novoServico.setValor(new BigDecimal("50.00"));
        novoServico.setAtivo(true);
        novoServico.setEstabelecimento(estabelecimento);
        var servico = servicoRepository.saveAndFlush(novoServico);
        servicoId = servico.getId();
        
        var profissional = new Profissional();
        profissional.setNome("Carlos");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setServicos(List.of(servico));
        
        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(1); // Segunda-feira
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(12, 0));
        jornada.setProfissional(profissional);
        profissional.getJornadasTrabalho().add(jornada);
        
        profissional = profissionalRepository.saveAndFlush(profissional);
        profissionalId = profissional.getId();

        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 11, 2, 9, 0)); // Segunda-feira 09:00
        agendamento.setDataHoraFim(LocalDateTime.of(2026, 11, 2, 9, 30));
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
        agendamento = agendamentoRepository.saveAndFlush(agendamento);
        agendamentoId = agendamento.getId();
    }

    @Test
    void deveAlterarStatusParaConfirmado() {
        gestorService.alterarStatus(estabelecimentoId, agendamentoId, StatusAgendamento.CONFIRMADO, emailGestor);
        
        var atualizado = agendamentoRepository.findById(agendamentoId).orElseThrow();
        assertEquals(StatusAgendamento.CONFIRMADO, atualizado.getStatus());
    }

    @Test
    void deveLancarErroAoTentarTransicaoInvalida() {
        // Passa de AGUARDANDO para CANCELADO (Válido)
        gestorService.alterarStatus(estabelecimentoId, agendamentoId, StatusAgendamento.CANCELADO, emailGestor);
        
        // Tenta passar de CANCELADO para CONCLUIDO (Inválido)
        var erro = assertThrows(ResponseStatusException.class, () -> 
            gestorService.alterarStatus(estabelecimentoId, agendamentoId, StatusAgendamento.CONCLUIDO, emailGestor)
        );
        assertEquals(400, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("Não é possível alterar o status"));
    }

    @Test
    void deveReagendarComSucesso() {
        LocalDateTime novaData = LocalDateTime.of(2026, 11, 2, 10, 0);
        gestorService.reagendar(estabelecimentoId, agendamentoId, novaData, emailGestor);
        
        var atualizado = agendamentoRepository.findById(agendamentoId).orElseThrow();
        assertEquals(novaData, atualizado.getDataHoraInicio());
        assertEquals(novaData.plusMinutes(30), atualizado.getDataHoraFim());
    }

    @Test
    void deveLancarErroDeConflitoAoReagendarParaHorarioOcupado() {
        // Criar uma reserva às 10:00 para causar colisão
        var cliente2 = usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente 2", "c2@teste.com", "hash", "CLIENTE"));
        var outroAgendamento = new Agendamento();
        outroAgendamento.setCliente(cliente2);
        outroAgendamento.setProfissional(profissionalRepository.findById(profissionalId).orElseThrow());
        outroAgendamento.setServico(servicoRepository.findById(servicoId).orElseThrow());
        outroAgendamento.setDataHoraInicio(LocalDateTime.of(2026, 11, 2, 10, 0));
        outroAgendamento.setDataHoraFim(LocalDateTime.of(2026, 11, 2, 10, 30));
        outroAgendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.saveAndFlush(outroAgendamento);

        // Tentar reagendar a nossa reserva das 09:00 para as 10:00
        var erro = assertThrows(ResponseStatusException.class, () -> 
            gestorService.reagendar(estabelecimentoId, agendamentoId, LocalDateTime.of(2026, 11, 2, 10, 0), emailGestor)
        );
        assertEquals(409, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("O novo horário selecionado colide com outra reserva."));
    }

    @Test
    void deveAtualizarDisponibilidadeAutomaticamenteAposCancelamento() {
        LocalDate data = LocalDate.of(2026, 11, 2);
        
        // Antes de cancelar, o horário das 09:00 está ocupado
        List<String> horariosAntes = disponibilidadeService.consultarHorarios(estabelecimentoId, servicoId, profissionalId, data);
        assertFalse(horariosAntes.contains("09:00"));
        
        // O Gestor cancela o agendamento
        gestorService.alterarStatus(estabelecimentoId, agendamentoId, StatusAgendamento.CANCELADO, emailGestor);
        
        // A Matemática de Intervalos relê os dados e o horário das 09:00 volta a aparecer imediatamente
        List<String> horariosDepois = disponibilidadeService.consultarHorarios(estabelecimentoId, servicoId, profissionalId, data);
        assertTrue(horariosDepois.contains("09:00"));
    }
}
