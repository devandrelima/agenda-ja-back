package com.agendaja.backend.integration;

import com.agendaja.backend.config.TestcontainersConfiguration;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class IsolamentoEstabelecimentosIntegracaoTest {
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private static final String SENHA_PADRAO = "SenhaSegura123";
    private static final String EMAIL_GESTOR_A = "gestor.a.isolamento@teste.com";
    private static final String EMAIL_CLIENTE_A = "cliente.a.isolamento@teste.com";

    private Long estAId;
    private Long estBId;
    private Long servicoBId;
    private Long profAId;
    private Long profBId;
    private Long agendamentoBId;
    private LocalDate proximaSegunda;

    private record SessaoCsrf(MockHttpSession session, String token) {}

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        proximaSegunda = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        String hash = passwordEncoder.encode(SENHA_PADRAO);

        // Estabelecimento A
        var estA = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão A", "SALAO", null, "Rua A"));
        estAId = estA.getId();
        usuarioRepository.saveAndFlush(new Usuario(null, estA, "Gestor A", EMAIL_GESTOR_A, hash, "GESTOR"));
        usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente A", EMAIL_CLIENTE_A, hash, "CLIENTE"));

        var servicoA = criarServico(estA, "Corte A");
        var profA = criarProfissionalComJornada(estA, "Profissional A", servicoA);
        profAId = profA.getId();

        // Estabelecimento B
        var estB = estabelecimentoRepository.saveAndFlush(new Estabelecimento(null, "Salão B", "SALAO", null, "Rua B"));
        estBId = estB.getId();
        usuarioRepository.saveAndFlush(new Usuario(null, estB, "Gestor B", "gestor.b.isolamento@teste.com", hash, "GESTOR"));
        var clienteB = usuarioRepository.saveAndFlush(new Usuario(null, null, "Cliente B", "cliente.b.isolamento@teste.com", hash, "CLIENTE"));

        var servicoB = criarServico(estB, "Corte B");
        servicoBId = servicoB.getId();
        var profB = criarProfissionalComJornada(estB, "Profissional B", servicoB);
        profBId = profB.getId();

        var agB = new Agendamento();
        agB.setCliente(clienteB);
        agB.setProfissional(profB);
        agB.setServico(servicoB);
        agB.setDataHoraInicio(proximaSegunda.atTime(10, 0));
        agB.setDataHoraFim(proximaSegunda.atTime(10, 30));
        agB.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
        agendamentoBId = agendamentoRepository.saveAndFlush(agB).getId();
    }

    private Servico criarServico(Estabelecimento est, String nome)
    {
        var servico = new Servico();
        servico.setNome(nome);
        servico.setDuracaoMinutos(30);
        servico.setValor(new BigDecimal("50.00"));
        servico.setAtivo(true);
        servico.setEstabelecimento(est);
        return servicoRepository.saveAndFlush(servico);
    }

    private Profissional criarProfissionalComJornada(Estabelecimento est, String nome, Servico servico)
    {
        var prof = new Profissional();
        prof.setNome(nome);
        prof.setEstabelecimento(est);
        prof.setServicos(List.of(servico));

        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(1);
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(18, 0));
        jornada.setProfissional(prof);
        prof.getJornadasTrabalho().add(jornada);

        return profissionalRepository.saveAndFlush(prof);
    }

    private SessaoCsrf autenticar(String email) throws Exception
    {
        MvcResult csrfRes = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) csrfRes.getRequest().getSession(false);
        String token = objectMapper.readTree(csrfRes.getResponse().getContentAsString())
                .path("token").asString("");

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", SENHA_PADRAO
                        ))))
                .andExpect(status().isOk());

        return new SessaoCsrf(session, token);
    }

    @Test
    void gestorANaoDeveAcessarNemAlterarRecursosAdministrativosDoEstabelecimentoB() throws Exception {
        var gestorA = autenticar(EMAIL_GESTOR_A);

        mockMvc.perform(get("/api/admin/estabelecimentos/{estId}/dashboard", estBId)
                        .session(gestorA.session()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/estabelecimentos/{estId}/agenda", estBId)
                        .param("inicio", proximaSegunda.atStartOfDay().toString())
                        .param("fim", proximaSegunda.atTime(23, 59).toString())
                        .session(gestorA.session()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/estabelecimentos/{estId}/servicos", estBId)
                        .session(gestorA.session()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/estabelecimentos/{estId}/agendamentos/{agId}/status", estBId, agendamentoBId)
                        .session(gestorA.session())
                        .header("X-CSRF-TOKEN", gestorA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELADO"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/estabelecimentos/{estId}/agendamentos/{agId}/horario", estBId, agendamentoBId)
                        .session(gestorA.session())
                        .header("X-CSRF-TOKEN", gestorA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dataHoraInicio", proximaSegunda.atTime(15, 0).toString()
                        ))))
                .andExpect(status().isForbidden());

        // Garante que a reserva do estabelecimento B permaneceu intacta no banco
        var reservaB = agendamentoRepository.findById(agendamentoBId).orElseThrow();
        assertEquals(StatusAgendamento.AGUARDANDO_CONFIRMACAO, reservaB.getStatus());
        assertEquals(proximaSegunda.atTime(10, 0), reservaB.getDataHoraInicio());
    }

    @Test
    void gestorANaoDeveManipularRecursosDeBInformandoOProprioEstabelecimentoId() throws Exception {
        var gestorA = autenticar(EMAIL_GESTOR_A);

        // Tentativas de IDOR usando estAId com IDs pertencentes ao Estabelecimento B -> 404
        mockMvc.perform(get("/api/admin/estabelecimentos/{estId}/servicos/{servicoId}", estAId, servicoBId)
                        .session(gestorA.session()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/admin/estabelecimentos/{estId}/agendamentos/{agId}/status", estAId, agendamentoBId)
                        .session(gestorA.session())
                        .header("X-CSRF-TOKEN", gestorA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMADO"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/admin/estabelecimentos/{estId}/agendamentos/{agId}/horario", estAId, agendamentoBId)
                        .session(gestorA.session())
                        .header("X-CSRF-TOKEN", gestorA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dataHoraInicio", proximaSegunda.atTime(15, 0).toString()
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/estabelecimentos/{estId}/agendamentos/{agId}", estAId, agendamentoBId)
                        .session(gestorA.session()))
                .andExpect(status().isNotFound());

        // Consulta direta no estabelecimento B pelo Gestor A -> 403
        mockMvc.perform(get("/api/estabelecimentos/{estId}/agendamentos/{agId}", estBId, agendamentoBId)
                        .session(gestorA.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteANaoDeveAcessarOuAlterarReservasDeOutroClienteNemMisturarEstabelecimentos() throws Exception {
        var clienteA = autenticar(EMAIL_CLIENTE_A);

        // Cliente não pode acessar área administrativa
        mockMvc.perform(get("/api/admin/estabelecimentos/{estId}/dashboard", estAId)
                        .session(clienteA.session()))
                .andExpect(status().isForbidden());

        // Cliente A não pode consultar, confirmar, cancelar nem reagendar reserva do Cliente B
        mockMvc.perform(get("/api/clientes/me/agendamentos/{agId}", agendamentoBId)
                        .session(clienteA.session()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/confirmacao", agendamentoBId)
                        .session(clienteA.session())
                        .header("X-CSRF-TOKEN", clienteA.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/cancelamento", agendamentoBId)
                        .session(clienteA.session())
                        .header("X-CSRF-TOKEN", clienteA.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/reagendamento", agendamentoBId)
                        .session(clienteA.session())
                        .header("X-CSRF-TOKEN", clienteA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "estabelecimentoId", estBId,
                                "servicoId", servicoBId,
                                "profissionalId", profBId,
                                "dataHoraInicio", proximaSegunda.atTime(16, 0).toString()
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/estabelecimentos/{estId}/agendamentos/{agId}", estBId, agendamentoBId)
                        .session(clienteA.session()))
                .andExpect(status().isForbidden());

        // Não deve permitir misturar profissional do Estabelecimento A com serviço do Estabelecimento B
        mockMvc.perform(post("/api/estabelecimentos/{estId}/agendamentos", estAId)
                        .session(clienteA.session())
                        .header("X-CSRF-TOKEN", clienteA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "servicoId", servicoBId,
                                "profissionalId", profAId,
                                "dataHoraInicio", proximaSegunda.atTime(11, 0).toString(),
                                "observacoes", "Tentativa cross-tenant"
                        ))))
                .andExpect(status().isNotFound());

        assertEquals(StatusAgendamento.AGUARDANDO_CONFIRMACAO,
                agendamentoRepository.findById(agendamentoBId).orElseThrow().getStatus());
    }
}
