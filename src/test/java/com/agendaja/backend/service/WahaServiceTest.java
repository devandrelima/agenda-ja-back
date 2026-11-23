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
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void naoConsultaWahaQuandoConfiguracaoEstaIncompleta() {
        ReflectionTestUtils.setField(service, "baseUrl", "   ");
        service.enviarConfirmacao(agendamento);

        ReflectionTestUtils.setField(service, "baseUrl", "http://agenda-ja-waha:3000");
        ReflectionTestUtils.setField(service, "apiKey", "   ");
        service.enviarConfirmacao(agendamento);

        ReflectionTestUtils.setField(service, "apiKey", "segredo-de-teste");
        ReflectionTestUtils.setField(service, "sessao", "   ");
        service.enviarConfirmacao(agendamento);

        verifyNoInteractions(gateway);
    }

    @Test
    void usaChatIdLidExatamenteComoDevolvidoPeloWaha() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_LID + "\"}");
        configurarRespostaDeEnvio();

        service.enviarConfirmacao(agendamento);

        JsonNode payload = capturarPayloadDeEnvio();
        assertThat(payload.path("chatId").asString("")).isEqualTo(CHAT_LID);
        verificarConsultaContato();
    }

    @Test
    void usaChatIdCusExatamenteComoDevolvidoPeloWaha() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarConfirmacao(agendamento);

        assertThat(capturarPayloadDeEnvio().path("chatId").asString("")).isEqualTo(CHAT_CUS);
    }

    @Test
    void orientaOClienteAConfirmarASolicitacao() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarSolicitacao(agendamento);

        String mensagem = capturarPayloadDeEnvio().path("text").asString("");
        assertThat(mensagem)
                .contains("Para confirmar sua presença, acesse o AgendaJá.")
                .doesNotContain("Aguarde a confirmação do estabelecimento");
    }

    @Test
    void enviaReagendamentoComTextoEsperado() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarReagendamento(agendamento);

        assertThat(capturarPayloadDeEnvio().path("text").asString(""))
                .isEqualTo("Olá, Maria! Seu agendamento de Corte com Ana foi reagendado para 25/11/2026 às 14:00.");
    }

    @Test
    void enviaLembreteComTextoObjetivo() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        service.enviarLembrete(agendamento);

        assertThat(capturarPayloadDeEnvio().path("text").asString(""))
                .isEqualTo("Olá, Maria! Lembrete: seu atendimento de Corte com Ana está marcado para 25/11/2026 às 14:00.");
    }

    @Test
    void normalizaTelefonesCom10E11DigitosComESemPrefixo55() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");
        configurarRespostaDeEnvio();

        for (String telefone : List.of("(84) 3312-4567", "(84) 99999-1234", "558433124567", "5584999991234")) {
            agendamento.getCliente().setTelefone(telefone);
            service.enviarConfirmacao(agendamento);
        }

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(gateway, times(8)).executar(eq("GET"), uriCaptor.capture(), anyString(), isNull(), any());

        List<String> consultasContato = uriCaptor.getAllValues().stream()
                .filter(uri -> uri.getPath().equals("/api/contacts/check-exists"))
                .map(uri -> uri.getQuery())
                .toList();

        assertThat(consultasContato).containsExactly(
                "phone=558433124567&session=agendaja",
                "phone=5584999991234&session=agendaja",
                "phone=558433124567&session=agendaja",
                "phone=5584999991234&session=agendaja"
        );
    }

    @Test
    void naoEnviaQuandoTelefoneEInvalido() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"WORKING\"}"));

        // Nulo, 12 dígitos sem prefixo 55 e 14 dígitos com prefixo 55
        agendamento.getCliente().setTelefone(null);
        service.enviarConfirmacao(agendamento);

        agendamento.getCliente().setTelefone("118499999123");
        service.enviarConfirmacao(agendamento);

        agendamento.getCliente().setTelefone("55849999912340");
        service.enviarConfirmacao(agendamento);

        verify(gateway, never()).executar(eq("GET"), argThat(uri -> uri.getPath().contains("check-exists")), anyString(), isNull(), any());
        verificarSemEnvio();
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

        configurarSessaoEContato(200, "{\"numberExists\":\"sim\",\"chatId\":\"" + CHAT_LID + "\"}");
        service.enviarConfirmacao(agendamento);

        verificarSemEnvio();
    }

    @Test
    void naoEnviaQuandoCheckExistsRecusaCredencialOuRetornaErro() throws Exception {
        for (int status : List.of(401, 404, 500, 400)) {
            configurarSessaoEContato(status, "");
            service.enviarConfirmacao(agendamento);
        }

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
    void naoConsultaContatoQuandoSessaoRetornaErroHttpSemEstadoOuExcecao() throws Exception {
        // Cobre todos os ramos de registrarErro (401, 404, 500 e 400)
        for (int status : List.of(401, 404, 500, 400)) {
            when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                    .thenReturn(new WahaHttpResponse(status, "{}"));
            service.enviarConfirmacao(agendamento);
        }

        // Status em branco ("sem estado") e fallback para "state"
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"   \"}"));
        service.enviarConfirmacao(agendamento);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"state\":\"STARTING\"}"));
        service.enviarConfirmacao(agendamento);

        // Exceção genérica
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenThrow(new IOException("falha de rede"));
        service.enviarConfirmacao(agendamento);

        // Interrupção de thread
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenThrow(new InterruptedException("interrompido"));
        service.enviarConfirmacao(agendamento);
        assertThat(Thread.interrupted()).isTrue();

        verificarSemEnvio();
    }

    @Test
    void preservaFluxoQuandoCheckExistsDaTimeoutOuLancaExcecao() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenAnswer(invocacao -> {
                    URI uri = invocacao.getArgument(1);
                    if (uri.getPath().contains("/api/sessions/")) return new WahaHttpResponse(200, "{\"status\":\"WORKING\"}");
                    throw new java.net.http.HttpTimeoutException("timeout");
                });
        service.enviarConfirmacao(agendamento);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenAnswer(invocacao -> {
                    URI uri = invocacao.getArgument(1);
                    if (uri.getPath().contains("/api/sessions/")) return new WahaHttpResponse(200, "{\"status\":\"WORKING\"}");
                    throw new IOException("erro de rede");
                });
        service.enviarConfirmacao(agendamento);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenAnswer(invocacao -> {
                    URI uri = invocacao.getArgument(1);
                    if (uri.getPath().contains("/api/sessions/")) return new WahaHttpResponse(200, "{\"status\":\"WORKING\"}");
                    throw new InterruptedException("interrompido");
                });
        service.enviarConfirmacao(agendamento);
        assertThat(Thread.interrupted()).isTrue();

        verificarSemEnvio();
    }

    @Test
    void trataFalhasHttpEExcecoesNoSendTextSemPropagar() throws Exception {
        configurarSessaoEContato(200, "{\"numberExists\":true,\"chatId\":\"" + CHAT_CUS + "\"}");

        for (int status : List.of(401, 404, 500, 422)) {
            when(gateway.executar(eq("POST"), any(), anyString(), anyString(), any()))
                    .thenReturn(new WahaHttpResponse(status, "erro"));
            assertDoesNotThrow(() -> service.enviarLembrete(agendamento));
        }

        when(gateway.executar(eq("POST"), any(), anyString(), anyString(), any()))
                .thenThrow(new IOException("queda de conexão"));
        assertDoesNotThrow(() -> service.enviarLembrete(agendamento));

        when(gateway.executar(eq("POST"), any(), anyString(), anyString(), any()))
                .thenThrow(new InterruptedException("interrompido"));
        assertDoesNotThrow(() -> service.enviarLembrete(agendamento));
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void consultaStatusDaSessaoComFallbacksEErrosAdministrativos() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"WORKING\"}"));
        assertThat(service.consultarStatusDaSessao()).isEqualTo("WORKING");

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"state\":\"SCAN_QR_CODE\"}"));
        assertThat(service.consultarStatusDaSessao()).isEqualTo("SCAN_QR_CODE");

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{}"));
        assertThat(service.consultarStatusDaSessao()).isEqualTo("UNKNOWN");

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{json-invalido"));
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(502);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(401, "{}"));
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(502);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(404, "{}"));
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(404);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(500, "{}"));
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(502);

        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenThrow(new IOException("offline"));
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(502);

        ReflectionTestUtils.setField(service, "apiKey", "");
        assertThat(assertThrows(ResponseStatusException.class, () -> service.consultarStatusDaSessao())
                .getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void consultaQrCodeRetornandoBytesDaImagem() throws Exception {
        ReflectionTestUtils.setField(service, "baseUrl", "http://agenda-ja-waha:3000/");
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};

        when(gateway.executar(
                eq("GET"),
                eq(URI.create("http://agenda-ja-waha:3000/api/agendaja/auth/qr")),
                eq("segredo-de-teste"),
                isNull(),
                eq(Duration.ofMillis(10000L))
        )).thenReturn(new WahaHttpResponse(200, png));

        assertThat(service.consultarQrCode()).isEqualTo(png);
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

            String mensagens = logs.list.stream()
                    .map(evento -> evento.getFormattedMessage())
                    .collect(Collectors.joining());
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
        assertThat(payload.path("session").asString("")).isEqualTo("agendaja");
        return payload;
    }

    private void verificarConsultaContato() throws Exception {
        verify(gateway).executar(eq("GET"), argThat(uri -> uri.toString().equals("http://agenda-ja-waha:3000/api/contacts/check-exists?phone=5584999991234&session=agendaja")), anyString(), isNull(), any());
    }

    private void verificarSemEnvio() throws Exception {
        verify(gateway, never()).executar(eq("POST"), any(), anyString(), anyString(), any());
    }
}
