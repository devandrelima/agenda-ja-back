package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoDetalhesDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {
    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Estabelecimento estabelecimento;
    private Agendamento agendamento;
    private Usuario clienteDono;
    private Usuario gestor;

    @BeforeEach
    void setup() {
        estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);
        estabelecimento.setNome("Salão Teste");

        clienteDono = new Usuario();
        clienteDono.setId(10L);
        clienteDono.setEmail("cliente@teste.com");
        clienteDono.setRole("CLIENTE");

        gestor = new Usuario();
        gestor.setId(20L);
        gestor.setEmail("gestor@teste.com");
        gestor.setRole("GESTOR");
        gestor.setEstabelecimento(estabelecimento);

        var servico = new Servico();
        servico.setId(100L);
        servico.setNome("Corte");
        servico.setValor(new BigDecimal("50.00"));

        var profissional = new Profissional();
        profissional.setId(200L);
        profissional.setNome("João");
        profissional.setEstabelecimento(estabelecimento);

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCliente(clienteDono);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 12, 1, 10, 0));
        agendamento.setDataHoraFim(LocalDateTime.of(2026, 12, 1, 10, 30));
    }

    @Test
    void consultarDetalhes_devePermitirAcessoAoClienteDono() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(usuarioRepository.findByEmail("cliente@teste.com")).thenReturn(Optional.of(clienteDono));

        AgendamentoDetalhesDTO dto = agendamentoService.consultarDetalhes(1L, 1L, "cliente@teste.com");

        assertNotNull(dto);
        assertEquals("Salão Teste", dto.estabelecimentoNome());
        assertEquals("Corte", dto.servicoNome());
    }

    @Test
    void consultarDetalhes_devePermitirAcessoAoGestorDoEstabelecimento() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestor));

        AgendamentoDetalhesDTO dto = agendamentoService.consultarDetalhes(1L, 1L, "gestor@teste.com");

        assertNotNull(dto);
        assertEquals("João", dto.profissionalNome());
    }

    @Test
    void consultarDetalhes_deveNegarAcessoSeClienteNaoForODono() {
        var outroCliente = new Usuario();
        outroCliente.setId(11L);
        outroCliente.setEmail("outro@teste.com");
        outroCliente.setRole("CLIENTE");

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(usuarioRepository.findByEmail("outro@teste.com")).thenReturn(Optional.of(outroCliente));

        assertThrows(ResponseStatusException.class, 
                () -> agendamentoService.consultarDetalhes(1L, 1L, "outro@teste.com"));
    }

    @Test
    void consultarDetalhes_deveNegarAcessoSeGestorForDeOutroEstabelecimento() {
        var outroEstabelecimento = new Estabelecimento();
        outroEstabelecimento.setId(2L);

        var outroGestor = new Usuario();
        outroGestor.setId(21L);
        outroGestor.setEmail("outro.gestor@teste.com");
        outroGestor.setRole("GESTOR");
        outroGestor.setEstabelecimento(outroEstabelecimento);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(usuarioRepository.findByEmail("outro.gestor@teste.com")).thenReturn(Optional.of(outroGestor));

        assertThrows(ResponseStatusException.class, 
                () -> agendamentoService.consultarDetalhes(1L, 1L, "outro.gestor@teste.com"));
    }
}
