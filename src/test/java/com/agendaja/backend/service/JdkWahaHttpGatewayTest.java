package com.agendaja.backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdkWahaHttpGatewayTest {
    @Test
    void deveForcarHttp1NoCliente() {
        var gateway = new JdkWahaHttpGateway(5000);

        assertThat(gateway.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void deveEnviarGetSemCorpoComChaveDaApi() throws Exception {
        HttpClient cliente = mock(HttpClient.class);
        HttpResponse<byte[]> resposta = mock();
        when(resposta.statusCode()).thenReturn(200);
        when(resposta.body()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(cliente.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(resposta);
        var gateway = new JdkWahaHttpGateway(cliente);

        gateway.executar("GET", URI.create("http://waha.test/api/sessions/agendaja"), "chave-secreta", null, Duration.ofSeconds(2));

        HttpRequest requisicao = capturarRequisicao(cliente);
        assertThat(requisicao.method()).isEqualTo("GET");
        assertThat(requisicao.version()).contains(HttpClient.Version.HTTP_1_1);
        assertThat(requisicao.headers().firstValue("X-Api-Key")).contains("chave-secreta");
        assertThat(requisicao.bodyPublisher()).isEmpty();
    }

    @Test
    void deveEnviarPostJsonComHttp1() throws Exception {
        HttpClient cliente = mock(HttpClient.class);
        HttpResponse<byte[]> resposta = mock();
        when(resposta.statusCode()).thenReturn(201);
        when(resposta.body()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(cliente.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(resposta);
        var gateway = new JdkWahaHttpGateway(cliente);

        gateway.executar("POST", URI.create("http://waha.test/api/sendText"), "chave-secreta", "{\"text\":\"oi\"}", Duration.ofSeconds(2));

        HttpRequest requisicao = capturarRequisicao(cliente);
        assertThat(requisicao.method()).isEqualTo("POST");
        assertThat(requisicao.version()).contains(HttpClient.Version.HTTP_1_1);
        assertThat(requisicao.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(requisicao.headers().firstValue("X-Api-Key")).contains("chave-secreta");
        assertThat(requisicao.bodyPublisher()).isPresent();
    }

    private HttpRequest capturarRequisicao(HttpClient cliente) throws Exception
    {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(cliente).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        return captor.getValue();
    }
}
