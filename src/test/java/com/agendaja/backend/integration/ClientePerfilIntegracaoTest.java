package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.ClienteAtualizacaoDTO;
import com.agendaja.backend.exception.EmailJaCadastradoException;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import com.agendaja.backend.security.UsuarioDetailsService;
import com.agendaja.backend.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientePerfilIntegracaoTest {
    @Autowired private ClienteService clienteService;
    @Autowired private UsuarioDetailsService usuarioDetailsService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;

    private Usuario clienteAlvo;
    private Usuario outroCliente;
    private Long idAgendamentoPassado;
    private Long idAgendamentoFuturo;

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();

        // Clientes
        clienteAlvo = new Usuario(null, null, "Cliente Alvo", "alvo@teste.com", "hash", "CLIENTE");
        clienteAlvo.setTelefone("84999990001");
        clienteAlvo = usuarioRepository.saveAndFlush(clienteAlvo);

        outroCliente = new Usuario(null, null, "Outro Cliente", "outro@teste.com", "hash", "CLIENTE");
        outroCliente = usuarioRepository.saveAndFlush(outroCliente);

        // Estrutura do Negócio
        var estabelecimento = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão", "SALAO", null, "Rua A"));
        
        Servico novoServico = new Servico();
        novoServico.setNome("Corte");
        novoServico.setDuracaoMinutos(30);
        novoServico.setValor(new BigDecimal("50.00"));
        novoServico.setAtivo(true);
        novoServico.setEstabelecimento(estabelecimento);
        var servico = servicoRepository.saveAndFlush(novoServico);
        
        var profissional = new Profissional();
        profissional.setNome("Carlos");
        profissional.setEstabelecimento(estabelecimento);
        profissional = profissionalRepository.saveAndFlush(profissional);

        // Agendamento Passado (Concluído)
        var agendamentoPassado = new Agendamento();
        agendamentoPassado.setCliente(clienteAlvo);
        agendamentoPassado.setProfissional(profissional);
        agendamentoPassado.setServico(servico);
        agendamentoPassado.setDataHoraInicio(LocalDateTime.now().minusDays(5));
        agendamentoPassado.setDataHoraFim(LocalDateTime.now().minusDays(5).plusMinutes(30));
        agendamentoPassado.setStatus(StatusAgendamento.CONCLUIDO);
        idAgendamentoPassado = agendamentoRepository.saveAndFlush(agendamentoPassado).getId();

        // Agendamento Futuro (Confirmado)
        var agendamentoFuturo = new Agendamento();
        agendamentoFuturo.setCliente(clienteAlvo);
        agendamentoFuturo.setProfissional(profissional);
        agendamentoFuturo.setServico(servico);
        agendamentoFuturo.setDataHoraInicio(LocalDateTime.now().plusDays(5));
        agendamentoFuturo.setDataHoraFim(LocalDateTime.now().plusDays(5).plusMinutes(30));
        agendamentoFuturo.setStatus(StatusAgendamento.CONFIRMADO);
        idAgendamentoFuturo = agendamentoRepository.saveAndFlush(agendamentoFuturo).getId();
    }

    @Test
    void deveAtualizarDadosDoPerfilComSucesso() {
        var dto = new ClienteAtualizacaoDTO("Novo Nome", "(84) 9 8888-7777", "  NOVO@email.com  ", null);
        
        var resposta = clienteService.atualizarPerfil(dto, clienteAlvo.getEmail());

        assertEquals("Novo Nome", resposta.nome());
        assertEquals("novo@email.com", resposta.email());
        assertEquals("84988887777", resposta.telefone());

        var noBanco = usuarioRepository.findById(clienteAlvo.getId()).orElseThrow();
        assertEquals("Novo Nome", noBanco.getNome());
    }

    @Test
    void deveImpedirAtualizacaoParaEmailJaExistente() {
        // Tenta usar o e-mail que já pertence ao "Outro Cliente"
        var dto = new ClienteAtualizacaoDTO("Novo Nome", "84988887777", outroCliente.getEmail(), null);

        var erro = assertThrows(EmailJaCadastradoException.class, () -> 
            clienteService.atualizarPerfil(dto, clienteAlvo.getEmail())
        );
        assertEquals("Este e-mail já está sendo utilizado por outra conta.", erro.getMessage());
    }

    @Test
    void deveDesativarContaECancelarApenasAgendamentosFuturos() {
        // Ação: Desativar a conta
        clienteService.desativarConta(clienteAlvo.getEmail());

        // Verificação 1: O utilizador está inativo
        var clienteNoBanco = usuarioRepository.findById(clienteAlvo.getId()).orElseThrow();
        assertFalse(clienteNoBanco.isAtivo());

        // Verificação 2: A marcação passada tem de permanecer intacta (CONCLUIDO)
        var passado = agendamentoRepository.findById(idAgendamentoPassado).orElseThrow();
        assertEquals(StatusAgendamento.CONCLUIDO, passado.getStatus());

        // Verificação 3: A marcação futura tem de ser cancelada
        var futuro = agendamentoRepository.findById(idAgendamentoFuturo).orElseThrow();
        assertEquals(StatusAgendamento.CANCELADO, futuro.getStatus());
    }

    @Test
    void deveBloquearLoginParaContaDesativada() {
        clienteService.desativarConta(clienteAlvo.getEmail());

        var erro = assertThrows(UsernameNotFoundException.class, () -> 
            usuarioDetailsService.loadUserByUsername(clienteAlvo.getEmail())
        );
        assertEquals("Esta conta foi desativada.", erro.getMessage());
    }
}
