package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.Usuario;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WahaServiceTest {
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
    void naoConsultaWahaQuandoCredencialEstaAusente() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        service.enviarConfirmacao(agendamento);
        verifyNoInteractions(gateway);
    }

    @Test
    void naoEnviaQuandoSessaoEstaEmScanQrOuFailed() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"SCAN_QR\"}"))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"FAILED\"}"));

        service.enviarConfirmacao(agendamento);
        service.enviarConfirmacao(agendamento);

        verify(gateway, times(2)).executar(eq("GET"), any(), anyString(), isNull(), any());
        verify(gateway, never()).executar(eq("POST"), any(), anyString(), anyString(), any());
    }

    @Test
    void naoEnviaQuandoSessaoNaoExisteOuCredencialERecusada() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(404, ""))
                .thenReturn(new WahaHttpResponse(401, ""));

        service.enviarConfirmacao(agendamento);
        service.enviarConfirmacao(agendamento);

        verify(gateway, never()).executar(eq("POST"), any(), anyString(), anyString(), any());
    }

    @Test
    void ignoraTimeoutEServicoIndisponivelSemDerrubarFluxo() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenThrow(new java.net.http.HttpTimeoutException("timeout"))
                .thenReturn(new WahaHttpResponse(503, ""));

        service.enviarConfirmacao(agendamento);
        service.enviarConfirmacao(agendamento);

        verify(gateway, never()).executar(eq("POST"), any(), anyString(), anyString(), any());
    }

    @Test
    void enviaPayloadCorretoSomenteComSessaoWorking() throws Exception {
        when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                .thenReturn(new WahaHttpResponse(200, "{\"status\":\"WORKING\"}"));
        when(gateway.executar(eq("POST"), any(), anyString(), anyString(), any()))
                .thenReturn(new WahaHttpResponse(201, "{}"));

        service.enviarConfirmacao(agendamento);

        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(gateway).executar(eq("POST"), uri.capture(), chave.capture(), corpo.capture(), any());
        JsonNode payload = objectMapper.readTree(corpo.getValue());
        assertThat(uri.getValue().toString()).isEqualTo("http://agenda-ja-waha:3000/api/sendText");
        assertThat(chave.getValue()).isEqualTo("segredo-de-teste");
        assertThat(payload.path("session").asText()).isEqualTo("agendaja");
        assertThat(payload.path("chatId").asText()).isEqualTo("5584999991234@c.us");
        assertThat(payload.path("text").asText()).contains("Maria", "Corte", "Ana");
    }

    @Test
    void nuncaRegistraChaveTelefoneOuConteudoDaMensagem() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WahaService.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            when(gateway.executar(eq("GET"), any(), anyString(), isNull(), any()))
                    .thenReturn(new WahaHttpResponse(401, "credencial inválida"));

            service.enviarConfirmacao(agendamento);

            String mensagens = logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
            assertThat(mensagens).doesNotContain("segredo-de-teste", "84999991234", "Maria", "Corte");
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
