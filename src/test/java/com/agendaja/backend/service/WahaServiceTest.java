package com.agendaja.backend.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WahaServiceTest {
    private static final String CHAT_LID = "5584999991234@lid";
    private static final String CHAT_CUS = "5584999991234@c.us";

    @Mock private WahaHttpGateway gateway;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WahaService service;
    private Agendamento agendamento;

    @BeforeEach
    void setup() {
        service = new WahaService(objectMapper, gateway);
        ReflectionTestUtils.setField(service, "habilitado", true);
        ReflectionTestUtils.setField(service, "baseUrl", "http://agenda-ja-waha:3000");
        ReflectionTestUtils.setField(service, "apiKey", "segredo-de-teste");
        ReflectionTestUtils.setField(service, "sessao", "agendaja");
        ReflectionTestUtils.setField(service, "readTimeoutMs", 10000L);

        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        cliente.setTelefone("(84) 99999-1234");
        var profissional = new Profissional();
        profissional.setNome("Ana");
        var servico = new Servico();
        servico.setNome("Corte");
        agendamento = new Agendamento();
        agendamento.setId(10L);
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(LocalDateTime.of(2026, 11, 25, 14, 0));
    }

    @Test
    void naoConsultaWahaQuandoIntegracaoEstaDesabilitada() {
        ReflectionTestUtils.setField(service, "habilitado", false);

        service.enviarConfirmacao(agendamento);

        verifyNoInteractions(gateway);
    }

    @Test
    void usaChatIdLidExatamenteComoDevolvidoPeloWaha() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_LID + "\"}");
        configurarRespostaDeEnvio();

        service.enviarConfirmacao(agendamento);

        JsonNode payload = capturarPayloadDeEnvio();
        assertThat(payload.path("chatId").asText()).isEqualTo(CHAT_LID);
        verificarConsultaContato();
    }

    @Test
    void usaChatIdCusExatamenteComoDevolvidoPeloWaha() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarConfirmacao(agendamento);

        assertThat(capturarPayloadDeEnvio().path("chatId").asText()).isEqualTo(CHAT_CUS);
    }

    @Test
    void orientaOClienteAConfirmarASolicitacao() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarSolicitacao(agendamento);

        String mensagem = capturarPayloadDeEnvio().path("text").asText();
        assertThat(mensagem)
                .contains("Para confirmar sua presença, acesse o AgendaJá.")
                .doesNotContain("Aguarde a confirmação do estabelecimento");
    }

    @Test
    void enviaLembreteComTextoObjetivo() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarLembrete(agendamento);

        assertThat(capturarPayloadDeEnvio().path("text").asText())
                .isEqualTo("Olá, Maria! Lembrete: seu atendimento de Corte com Ana está marcado para 25/11/2026 às 14:00.");
    }

    @Test
    void naoEnviaQuandoNumeroNaoPossuiWhatsApp() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":false}");

        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void naoEnviaQuandoChatIdEstaAusenteOuVazio() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true}");
        service.enviarConfirmacao(agendamento);
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"  \"}");
        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void naoEnviaQuandoRespostaDeContatoEInvalida() throws Exception {
        configurarSessaoEContato(200, "{\"chatId\":\"" + CHAT_LID + "\"}");

        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void naoEnviaQuandoCheckExistsRecusaCredencialOuRetornaErro() throws Exception {
        configurarSessaoEContato(401, "");
        service.enviarConfirmacao(agendamento);
        configurarSessaoEContato(500, "");
        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void naoConsultaContatoQuandoSessaoNaoEstaWorking() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"SCAN_QR_CODE\"}"));

        service.enviarConfirmacao(agendamento);

        verify(gateway).executar(eq("GET"), argThat(uri -> uri.getPath().contains("/api/sessions/")), anyString(), isNull(), any());
        verify(gateway, never()).executar(eq("GET"), argThat(uri -> uri.getPath().contains("check-exists")), anyString(), isNull(), any());
        verificarSemEnvio();
    }

    @Test
    void preservaFluxoQuandoCheckExistsDaTimeout() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenAnswer(invocacao -> {
                    URI uri = invocacao.getArgument(1);
                    if (uri.getPath().contains("/api/sessions/")) return new WahaHttpResponse(200, "{\"status\":\"WORKING\"}");
                    throw new java.net.http.HttpTimeoutException("timeout");
                });

        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void nuncaRegistraChaveTelefoneChatIdOuMensagem() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WahaService.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            configurarSessaoEContato(401, "");
            service.enviarConfirmacao(agendamento);

            String mensagens = logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
            assertThat(mensagens).doesNotContain("segredo-de-teste", "84999991234", CHAT_LID, "Maria", "Corte");
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    private void configurarSessaoEContato(int statusContato, String corpoContato) throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenAnswer(invocacao -> {
                    URI uri = invocacao.getArgument(1);
                    if (uri.getPath().contains("/api/sessions/")) return new WahaHttpResponse(200, "{\"status\":\"WORKING\"}");
                    return new WahaHttpResponse(statusContato, corpoContato);
                });
    }

    private void configurarRespostaDeEnvio() throws Exception {
        when(gateway.executar(eq("POST"), any(), anyString(), anyString(), any()))
                .thenReturn(new WahaHttpResponse(201, "{}"));
    }

    private JsonNode capturarPayloadDeEnvio() throws Exception {
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(gateway).executar(eq("POST"), eq(URI.create("http://agenda-ja-waha:3000/api/sendText")), anyString(), corpo.capture(), any());
        JsonNode payload = objectMapper.readTree(corpo.getValue());
        assertThat(payload.path("session").asText()).isEqualTo("agendaja");
        return payload;
    }

    private void verificarConsultaContato() throws Exception {
        verify(gateway).executar(eq("GET"), argThat(uri -> uri.toString().equals("http://agenda-ja-waha:3000/api/contacts/check-exists?phone=5584999991234&session=agendaja")), anyString(), isNull(), any());
    }

    private void verificarSemEnvio() throws Exception {
        verify(gateway, never()).executar(eq("POST"), any(), anyString(), anyString(), any());
    }
}
