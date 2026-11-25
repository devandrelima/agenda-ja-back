package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoDetalhesDTO;
import com.agendaja.backend.dto.AgendamentoRequestDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.agendaja.backend.config.TimeConfig;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {
    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AgendamentoService agendamentoService;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ServicoRepository servicoRepository;

    private Estabelecimento estabelecimento;
    private Agendamento agendamento;
    private Usuario clienteDono;
    private Usuario gestor;

    private Clock clockFixo;
    private LocalDateTime agoraFixo;

    @BeforeEach
    void setup() {
        agoraFixo = LocalDateTime.of(2026, 11, 25, 14, 0);
        clockFixo = Clock.fixed(
                agoraFixo.atZone(TimeConfig.ZONA_PADRAO).toInstant(),
                TimeConfig.ZONA_PADRAO
        );

        agendamentoService = new AgendamentoService(
                agendamentoRepository,
                usuarioRepository,
                profissionalRepository,
                servicoRepository,
                eventPublisher,
                clockFixo
        );

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

    private Agendamento criarAgendamentoCompleto(Long estId, Long clienteId)
    {
        var est = new Estabelecimento(estId, "Salão", "SALAO", null, "Centro");

        var prof = new Profissional();
        prof.setId(2L);
        prof.setNome("Carlos");
        prof.setEstabelecimento(est);

        var servico = new Servico();
        servico.setId(3L);
        servico.setNome("Corte");
        servico.setDuracaoMinutos(30);
        servico.setValor(new java.math.BigDecimal("50.00"));
        servico.setEstabelecimento(est);
        prof.setServicos(List.of(servico));

        var cliente = new Usuario(clienteId, null, "Maria", "maria@teste.com", "hash", "CLIENTE");

        var ag = new Agendamento();
        ag.setId(10L);
        ag.setCliente(cliente);
        ag.setProfissional(prof);
        ag.setServico(servico);
        ag.setDataHoraInicio(LocalDateTime.of(2026, 12, 10, 14, 0));
        ag.setDataHoraFim(LocalDateTime.of(2026, 12, 10, 14, 30));
        ag.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
        return ag;
    }

    @Test
    void registrar_deveRetornar409EmConcurrencyFailureEValidarRecursosAusentes() {
        var dto = new AgendamentoRequestDTO(3L, 2L, LocalDateTime.of(2026, 12, 10, 14, 0), null);

        // Cliente não encontrado (401)
        when(usuarioRepository.findByEmail("x@teste.com")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dto, "x@teste.com")).getStatusCode().value());

        var ag = criarAgendamentoCompleto(1L, 100L);
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(ag.getCliente()));

        // Profissional não encontrado (404)
        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(2L, 1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dto, "maria@teste.com")).getStatusCode().value());

        // Serviço não encontrado (404)
        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(2L, 1L))
                .thenReturn(Optional.of(ag.getProfissional()));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(3L, 1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dto, "maria@teste.com")).getStatusCode().value());

        // ConcurrencyFailureException no flush (409)
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(3L, 1L))
                .thenReturn(Optional.of(ag.getServico()));
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(ag);
        doThrow(new org.springframework.dao.ConcurrencyFailureException("deadlock"))
                .when(agendamentoRepository).flush();

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dto, "maria@teste.com")).getStatusCode().value());
    }

    @Test
    void consultarDetalhes_deveCobrirTodosOsRamosDeAutorizacaoENaoEncontrado() {
        var ag = criarAgendamentoCompleto(1L, 100L);

        // Agendamento inexistente (404)
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(1L, 10L, "maria@teste.com")).getStatusCode().value());

        // Agendamento de outro estabelecimento (404)
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(ag));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(99L, 10L, "maria@teste.com")).getStatusCode().value());

        // Usuário não encontrado (401)
        when(usuarioRepository.findByEmail("ausente@teste.com")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(1L, 10L, "ausente@teste.com")).getStatusCode().value());

        // Gestor do mesmo estabelecimento -> autorizado
        var gestorMesmoEst = new Usuario(1L, ag.getProfissional().getEstabelecimento(), "Gestor", "g1@teste.com", "h", "GESTOR");
        when(usuarioRepository.findByEmail("g1@teste.com")).thenReturn(Optional.of(gestorMesmoEst));
        assertNotNull(agendamentoService.consultarDetalhes(1L, 10L, "g1@teste.com"));

        // Gestor sem estabelecimento vinculado -> 403
        var gestorSemEst = new Usuario(2L, null, "Gestor", "g2@teste.com", "h", "GESTOR");
        when(usuarioRepository.findByEmail("g2@teste.com")).thenReturn(Optional.of(gestorSemEst));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(1L, 10L, "g2@teste.com")).getStatusCode().value());

        // Gestor de outro estabelecimento -> 403
        var outroEst = new Estabelecimento(2L, "Outro", "SALAO", null, "Centro");
        var gestorOutroEst = new Usuario(3L, outroEst, "Gestor", "g3@teste.com", "h", "GESTOR");
        when(usuarioRepository.findByEmail("g3@teste.com")).thenReturn(Optional.of(gestorOutroEst));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(1L, 10L, "g3@teste.com")).getStatusCode().value());

        // Cliente dono do agendamento -> autorizado
        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(ag.getCliente()));
        assertNotNull(agendamentoService.consultarDetalhes(1L, 10L, "maria@teste.com"));

        // Outro cliente -> 403
        var outroCliente = new Usuario(200L, null, "João", "joao@teste.com", "h", "CLIENTE");
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(outroCliente));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> agendamentoService.consultarDetalhes(1L, 10L, "joao@teste.com")).getStatusCode().value());
    }

    @Test
    void registrar_deveRejeitarHorarioNoPassadoOuIgualAoAtualCom400() {
        var dtoPassado = new AgendamentoRequestDTO(3L, 2L, agoraFixo.minusMinutes(1), null);
        var dtoAgora = new AgendamentoRequestDTO(3L, 2L, agoraFixo, null);

        var erroPassado = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dtoPassado, "maria@teste.com"));
        var erroAgora = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.registrar(1L, dtoAgora, "maria@teste.com"));

        assertEquals(400, erroPassado.getStatusCode().value());
        assertEquals(400, erroAgora.getStatusCode().value());
        verifyNoInteractions(usuarioRepository, profissionalRepository, servicoRepository, agendamentoRepository, eventPublisher);
    }
}
