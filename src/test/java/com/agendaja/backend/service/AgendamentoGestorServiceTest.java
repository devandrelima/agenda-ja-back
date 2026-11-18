package com.agendaja.backend.service;

import com.agendaja.backend.event.AgendamentoReagendadoEvent;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoGestorServiceTest {
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private EstabelecimentoAuthorizationService authorizationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AgendamentoGestorService service;

    private Agendamento agendamento;

    @BeforeEach
    void setup() {
        var est = new Estabelecimento();
        est.setId(1L);

        var prof = new Profissional();
        prof.setId(2L);
        prof.setNome("Carlos");
        prof.setEstabelecimento(est);

        var servico = new Servico();
        servico.setId(3L);
        servico.setNome("Corte");
        servico.setDuracaoMinutos(30);

        var cliente = new Usuario(4L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");

        agendamento = new Agendamento();
        agendamento.setId(10L);
        agendamento.setCliente(cliente);
        agendamento.setProfissional(prof);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 12, 1, 10, 0));
        agendamento.setDataHoraFim(LocalDateTime.of(2026, 12, 1, 10, 30));
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
    }

    @Test
    void alterarStatus_deveRetornar404SeAgendamentoNaoExistir() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.alterarStatus(1L, 10L, StatusAgendamento.CONFIRMADO, "gestor@teste.com"));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void alterarStatus_deveRetornar404SePertencerAOutroEstabelecimento() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.alterarStatus(99L, 10L, StatusAgendamento.CONFIRMADO, "gestor@teste.com"));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void alterarStatus_deveRetornar400ParaTransicaoInvalida() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.alterarStatus(1L, 10L, StatusAgendamento.CONCLUIDO, "gestor@teste.com"));

        assertEquals(400, erro.getStatusCode().value());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void alterarStatus_deveAlterarEPublicarEvento() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        service.alterarStatus(1L, 10L, StatusAgendamento.CONFIRMADO, "gestor@teste.com");

        assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
        verify(eventPublisher).publishEvent(any(AgendamentoStatusAlteradoEvent.class));
    }

    @Test
    void reagendar_deveRetornar404SeNaoEncontradoOuDeOutroEstabelecimento() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.empty());
        var erroNaoEncontrado = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(1L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(404, erroNaoEncontrado.getStatusCode().value());

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        var erroOutroEst = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(99L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(404, erroOutroEst.getStatusCode().value());
    }

    @Test
    void reagendar_deveRetornar400QuandoCanceladoOuConcluido() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        var erroCancelado = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(1L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(400, erroCancelado.getStatusCode().value());

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        var erroConcluido = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(1L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(400, erroConcluido.getStatusCode().value());
    }

    @Test
    void reagendar_deveRetornar409EmColisaoDeIntegridadeOuConcorrencia() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        doThrow(new DataIntegrityViolationException("colisão")).when(agendamentoRepository).flush();
        var erroIntegridade = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(1L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(409, erroIntegridade.getStatusCode().value());

        doThrow(new ConcurrencyFailureException("deadlock")).when(agendamentoRepository).flush();
        var erroConcorrencia = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(1L, 10L, LocalDateTime.of(2026, 12, 2, 10, 0), "gestor@teste.com"));
        assertEquals(409, erroConcorrencia.getStatusCode().value());
    }

    @Test
    void reagendar_deveAtualizarHorarioEPublicarEvento() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        var novoInicio = LocalDateTime.of(2026, 12, 2, 15, 0);

        service.reagendar(1L, 10L, novoInicio, "gestor@teste.com");

        assertEquals(novoInicio, agendamento.getDataHoraInicio());
        assertEquals(novoInicio.plusMinutes(30), agendamento.getDataHoraFim());
        verify(agendamentoRepository).flush();
        verify(eventPublisher).publishEvent(any(AgendamentoReagendadoEvent.class));
    }

    @Test
    void listarAgenda_deveTratarStatusNuloVazioEPreenchido() {
        var inicio = LocalDateTime.of(2026, 12, 1, 0, 0);
        var fim = LocalDateTime.of(2026, 12, 7, 23, 59);
        when(agendamentoRepository.buscarAgendaAdministrativa(anyLong(), any(), any(), any(), anyList(), any()))
                .thenReturn(List.of(agendamento));

        // statusList == null
        var resNulo = service.listarAgenda(1L, inicio, fim, null, null, null, "gestor@teste.com");
        assertEquals(1, resNulo.size());

        // statusList vazio
        var resVazio = service.listarAgenda(1L, inicio, fim, null, List.of(), null, "gestor@teste.com");
        assertEquals(1, resVazio.size());

        // statusList preenchido
        var filtro = List.of(StatusAgendamento.CONFIRMADO);
        var resPreenchido = service.listarAgenda(1L, inicio, fim, 2L, filtro, null, "gestor@teste.com");
        assertEquals(1, resPreenchido.size());

        verify(agendamentoRepository, times(2)).buscarAgendaAdministrativa(
                eq(1L), eq(inicio), eq(fim), isNull(), eq(List.of(StatusAgendamento.values())), isNull());
        verify(agendamentoRepository).buscarAgendaAdministrativa(
                eq(1L), eq(inicio), eq(fim), eq(2L), eq(filtro), isNull());
    }
}
