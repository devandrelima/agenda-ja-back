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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AutenticacaoEReservaIntegracaoTest {
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Long estabelecimentoId;
    private Long servicoId;
    private Long profissionalId;
    private LocalDate proximaSegunda;

    private record SessaoCsrf(MockHttpSession session, String token) {}

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        proximaSegunda = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        var estabelecimento = estabelecimentoRepository.saveAndFlush(
                new Estabelecimento(null, "Barbearia HU25", "BARBEARIA", null, "Centro")
        );
        estabelecimentoId = estabelecimento.getId();

        var servico = new Servico();
        servico.setNome("Corte Social");
        servico.setDuracaoMinutos(30);
        servico.setValor(new BigDecimal("45.00"));
        servico.setAtivo(true);
        servico.setEstabelecimento(estabelecimento);
        servico = servicoRepository.saveAndFlush(servico);
        servicoId = servico.getId();

        var profissional = new Profissional();
        profissional.setNome("Lucas");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setServicos(List.of(servico));

        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(1); // Segunda-feira
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(18, 0));
        jornada.setProfissional(profissional);
        profissional.getJornadasTrabalho().add(jornada);

        profissional = profissionalRepository.saveAndFlush(profissional);
        profissionalId = profissional.getId();
    }

    private final List<String> emailsCriados = new ArrayList<>();

    @AfterEach
    void limpar() {
        agendamentoRepository.deleteAll();
        if (profissionalId != null) {
            profissionalRepository.deleteById(profissionalId);
        }
        if (servicoId != null) {
            servicoRepository.deleteById(servicoId);
        }
        for (String email : emailsCriados) {
            usuarioRepository.findByEmail(email)
                    .ifPresent(u -> usuarioRepository.deleteById(u.getId()));
        }
        emailsCriados.clear();
        if (estabelecimentoId != null) {
            estabelecimentoRepository.deleteById(estabelecimentoId);
        }
    }

    private SessaoCsrf obterCsrf(MockHttpSession sessaoExistente) throws Exception
    {
        var req = get("/api/auth/csrf");
        if (sessaoExistente != null) req.session(sessaoExistente);

        MvcResult result = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new SessaoCsrf(session, json.path("token").asString(""));
    }

    private SessaoCsrf cadastrarEAutenticarCliente(String nome, String telefone, String email, String senha) throws Exception 
    {
        var csrf = obterCsrf(null);

        mockMvc.perform(post("/api/clientes")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", nome,
                                "telefone", telefone,
                                "email", email,
                                "senha", senha
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", senha
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        return obterCsrf(csrf.session());
    }

    @Test
    void deveCadastrarClienteAutenticarPorEmailETelefoneConsultarSessaoEFazerLogout() throws Exception {
        String email = "auth.hu25@teste.com";
        String telefone = "(84) 99123-4567";
        String senha = "SenhaForte123";
        emailsCriados.add(email);

        // Escrita sem token CSRF deve ser bloqueada
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Ana Souza",
                                "telefone", telefone,
                                "email", email,
                                "senha", senha
                        ))))
                .andExpect(status().isForbidden());

        var csrf = obterCsrf(null);

        // Cadastro real com hash BCrypt persistido no Postgres
        mockMvc.perform(post("/api/clientes")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Ana Souza",
                                "telefone", telefone,
                                "email", email,
                                "senha", senha
                        ))))
                .andExpect(status().isCreated());

        // Senha incorreta deve retornar 401
        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", "SenhaErrada123"
                        ))))
                .andExpect(status().isUnauthorized());

        // Login por e-mail
        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", senha
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        // Sessão ativa em /api/auth/me
        mockMvc.perform(get("/api/auth/me").session(csrf.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Logout invalida a sessão
        var csrfLogado = obterCsrf(csrf.session());
        mockMvc.perform(post("/api/auth/logout")
                        .session(csrfLogado.session())
                        .header("X-CSRF-TOKEN", csrfLogado.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(csrfLogado.session()))
                .andExpect(status().isUnauthorized());

        // Novo login utilizando o telefone formatado
        var novaSessao = obterCsrf(null);
        mockMvc.perform(post("/api/auth/login")
                        .session(novaSessao.session())
                        .header("X-CSRF-TOKEN", novaSessao.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", telefone,
                                "senha", senha
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void deveBloquearLoginDeUsuarioInativoEAcessoAnonimoADetalhesDeReserva() throws Exception {
        String email = "inativo.hu25@teste.com";
        cadastrarEAutenticarCliente("Cliente Inativo", "(84) 99222-3333", email, "SenhaForte123");

        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setAtivo(false);
        usuarioRepository.saveAndFlush(usuario);

        var csrf = obterCsrf(null);
        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", "SenhaForte123"
                        ))))
                .andExpect(status().isUnauthorized());

        // Consulta anônima a detalhes de agendamento deve retornar 401 (e não 500)
        mockMvc.perform(get("/api/estabelecimentos/{estId}/agendamentos/{agId}", estabelecimentoId, 999L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExecutarFluxoCompletoDeReservaViaHttpComConfirmacaoReagendamentoECancelamento() throws Exception {
        var sessao = cadastrarEAutenticarCliente("Pedro Lima", "(84) 99333-4444", "pedro.hu25@teste.com", "SenhaForte123");
        emailsCriados.add("pedro.hu25@teste.com");

        // Rejeita horário no passado (400)
        LocalDateTime horarioPassado = LocalDateTime.now(clock).minusHours(2).withSecond(0).withNano(0);
        mockMvc.perform(post("/api/estabelecimentos/{estId}/agendamentos", estabelecimentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "servicoId", servicoId,
                                "profissionalId", profissionalId,
                                "dataHoraInicio", horarioPassado.toString(),
                                "observacoes", "Tentativa no passado"
                        ))))
                .andExpect(status().isBadRequest());

        // Cria reserva futura válida (201)
        LocalDateTime horarioInicial = proximaSegunda.atTime(10, 0);
        MvcResult criacaoResult = mockMvc.perform(post("/api/estabelecimentos/{estId}/agendamentos", estabelecimentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "servicoId", servicoId,
                                "profissionalId", profissionalId,
                                "dataHoraInicio", horarioInicial.toString(),
                                "observacoes", "Primeira visita"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                .andReturn();

        Long agendamentoId = objectMapper.readTree(criacaoResult.getResponse().getContentAsString())
                .path("id").asLong();

        // Consulta detalhes e listagem do cliente
        mockMvc.perform(get("/api/estabelecimentos/{estId}/agendamentos/{agId}", estabelecimentoId, agendamentoId)
                        .session(sessao.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicoNome").value("Corte Social"));

        mockMvc.perform(get("/api/clientes/me/agendamentos").session(sessao.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Confirma reserva pendente
        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/confirmacao", agendamentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        // Rejeita reagendamento para horário fora da jornada (409)
        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/reagendamento", agendamentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "estabelecimentoId", estabelecimentoId,
                                "servicoId", servicoId,
                                "profissionalId", profissionalId,
                                "dataHoraInicio", proximaSegunda.atTime(22, 0).toString()
                        ))))
                .andExpect(status().isConflict());

        assertEquals(StatusAgendamento.CONFIRMADO, agendamentoRepository.findById(agendamentoId).orElseThrow().getStatus());

        // Reagenda para horário livre dentro da jornada (201)
        LocalDateTime novoHorario = proximaSegunda.atTime(14, 0);
        MvcResult reagendamentoResult = mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/reagendamento", agendamentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "estabelecimentoId", estabelecimentoId,
                                "servicoId", servicoId,
                                "profissionalId", profissionalId,
                                "dataHoraInicio", novoHorario.toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long novoAgendamentoId = objectMapper.readTree(reagendamentoResult.getResponse().getContentAsString())
                .path("id").asLong();
        assertNotEquals(agendamentoId, novoAgendamentoId);
        assertEquals(StatusAgendamento.CANCELADO, agendamentoRepository.findById(agendamentoId).orElseThrow().getStatus());

        // Cancela a nova reserva (200)
        mockMvc.perform(post("/api/clientes/me/agendamentos/{agId}/cancelamento", novoAgendamentoId)
                        .session(sessao.session())
                        .header("X-CSRF-TOKEN", sessao.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }
}
