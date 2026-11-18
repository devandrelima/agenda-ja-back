package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.ClienteResumoProjection;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteGestorServiceTest {
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstabelecimentoAuthorizationService authorizationService;

    @InjectMocks private ClienteGestorService service;

    @Test
    void deveListarClientesDoEstabelecimento() {
        var projecao = mock(ClienteResumoProjection.class);
        when(projecao.getClienteId()).thenReturn(10L);
        when(projecao.getNome()).thenReturn("Maria Silva");
        when(projecao.getTelefone()).thenReturn("84999999999");
        when(projecao.getTotalVisitas()).thenReturn(3);
        var ultimaVisita = LocalDateTime.of(2026, 11, 10, 14, 0);
        when(projecao.getUltimaVisita()).thenReturn(ultimaVisita);

        when(agendamentoRepository.buscarResumoClientesPorEstabelecimento(1L))
                .thenReturn(List.of(projecao));

        var resultado = service.listarClientes(1L, "gestor@teste.com");

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).id());
        assertEquals("Maria Silva", resultado.get(0).nome());
        assertEquals(3, resultado.get(0).totalVisitas());
        assertEquals(ultimaVisita, resultado.get(0).ultimaVisita());
        verify(authorizationService).validarAcesso("gestor@teste.com", 1L);
    }

    @Test
    void deveRetornar404QuandoClienteNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.consultarHistorico(1L, 99L, "gestor@teste.com")
        );

        assertEquals(404, erro.getStatusCode().value());
        verify(authorizationService).validarAcesso("gestor@teste.com", 1L);
        verifyNoInteractions(agendamentoRepository);
    }

    @Test
    void deveRetornarNenhumQuandoClienteNaoPossuiAgendamentosNoEstabelecimento() {
        var cliente = new Usuario(10L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        cliente.setTelefone("84999999999");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.buscarHistoricoDoClienteNoEstabelecimento(1L, 10L))
                .thenReturn(List.of());

        var historico = service.consultarHistorico(1L, 10L, "gestor@teste.com");

        assertEquals("Nenhum", historico.servicoMaisFrequente());
        assertTrue(historico.agendamentos().isEmpty());
    }

    @Test
    void deveCalcularServicoMaisFrequenteCorretamente() {
        var cliente = new Usuario(10L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        cliente.setTelefone("84999999999");

        var corte = criarAgendamento(1L, "Corte", new BigDecimal("50.00"));
        var escova1 = criarAgendamento(2L, "Escova", new BigDecimal("70.00"));
        var escova2 = criarAgendamento(3L, "Escova", new BigDecimal("70.00"));

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.buscarHistoricoDoClienteNoEstabelecimento(1L, 10L))
                .thenReturn(List.of(corte, escova1, escova2));

        var historico = service.consultarHistorico(1L, 10L, "gestor@teste.com");

        assertEquals("Escova", historico.servicoMaisFrequente());
        assertEquals(3, historico.agendamentos().size());
    }

    private Agendamento criarAgendamento(Long id, String nomeServico, BigDecimal valor)
    {
        var servico = new Servico();
        servico.setNome(nomeServico);
        servico.setValor(valor);

        var prof = new Profissional();
        prof.setNome("Ana");

        var agendamento = new Agendamento();
        agendamento.setId(id);
        agendamento.setServico(servico);
        agendamento.setProfissional(prof);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 11, 15, 10, 0));
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return agendamento;
    }
}
