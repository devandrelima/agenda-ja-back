package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.dto.AgendamentoRequestDTO;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AgendamentoConcorrenciaIntegracaoTest {
    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ProfissionalRepository profissionalRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private Long estabelecimentoId;
    private Long servicoId;
    private Long profissionalId;
    private String emailCliente;

    @BeforeEach
    void setup() {
        agendamentoRepository.deleteAll();  // Limpar a tabela antes do teste

        // Setup de Estabelecimento
        var estabelecimento = estabelecimentoRepository.save(
                new Estabelecimento(null, "Salão Teste Concorrência", "SALÃO", null, "Rua A")
        );
        estabelecimentoId = estabelecimento.getId();

        // Setup de Cliente
        emailCliente = "cliente.concorrencia." + UUID.randomUUID() + "@teste.com";
        usuarioRepository.save(new Usuario(null, null, "Cliente Ávido", emailCliente, "hash", "CLIENTE"));

        // Setup de Serviço
        Servico novoServico = new Servico();
        novoServico.setNome("Corte");
        novoServico.setDuracaoMinutos(30);
        novoServico.setValor(new BigDecimal("45.00"));
        novoServico.setAtivo(true);
        novoServico.setEstabelecimento(estabelecimento);
        
        var servico = servicoRepository.save(novoServico);
        servicoId = servico.getId();

        // Setup de Profissional
        var profissional = new Profissional();
        profissional.setNome("João");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setServicos(List.of(servico));
        profissional = profissionalRepository.save(profissional);
        profissionalId = profissional.getId();
    }

    @Test
    void deveImpedirDoubleBookingComMultiplasThreadsSimultaneas() throws InterruptedException {
        int numeroDeRequisicoesSimultaneas = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numeroDeRequisicoesSimultaneas);
        
        // CountDownLatch funciona como uma "porta de partida" de uma corrida
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch concluidos = new CountDownLatch(numeroDeRequisicoesSimultaneas);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasDeConcorrencia = new AtomicInteger(0);

        LocalDateTime horarioDesejado = LocalDateTime.of(2026, 12, 24, 14, 0);
        AgendamentoRequestDTO requestDTO = new AgendamentoRequestDTO(servicoId, profissionalId, horarioDesejado, "Reserva de Natal");

        // 10 threads para atacarem o serviço ao mesmo tempo
        for (int i = 0; i < numeroDeRequisicoesSimultaneas; i++) {
            executor.submit(() -> {
                try {
                    largada.await();  // Todas as threads congelam à espera da "largada"
                    
                    agendamentoService.registrar(estabelecimentoId, requestDTO, emailCliente);
                    sucessos.incrementAndGet();
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {  // 409 Conflict lançado pelo catch no service
                        falhasDeConcorrencia.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    concluidos.countDown();
                }
            });
        }

        // Abre a porta de partida (todas as 10 requisições disparam no mesmo milissegundo)
        largada.countDown();
        
        // Aguarda que todas as threads terminem a execução
        concluidos.await();
        executor.shutdown();

        // Asserções para provar a consistência do sistema (ACID)
        assertEquals(1, sucessos.get(), "Apenas 1 agendamento deve ter sucesso");
        assertEquals(numeroDeRequisicoesSimultaneas - 1, falhasDeConcorrencia.get(), "As restantes 9 devem falhar com HTTP 409 (Conflict)");
        assertEquals(1, agendamentoRepository.count(), "A Restrição de Exclusão (banco de dados) garantiu que houvesse apenas 1 agendamento salvo");
    }
}
