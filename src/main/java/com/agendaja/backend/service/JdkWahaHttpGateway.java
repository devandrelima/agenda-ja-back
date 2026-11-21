package com.agendaja.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class JdkWahaHttpGateway implements WahaHttpGateway {
    private final HttpClient clienteHttp;

    @Autowired
    public JdkWahaHttpGateway(@Value("${app.waha.connect-timeout-ms:5000}") long connectTimeoutMs) {
        this(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build());
    }

    JdkWahaHttpGateway(HttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;
    }

    HttpClient.Version version() {
        return clienteHttp.version();
    }

    @Override
    public WahaHttpResponse executar(String metodo, URI uri, String apiKey, String corpo, Duration timeout) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json");
        if ("GET".equals(metodo)) {
            requisicao.GET();
        } else {
            requisicao.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        }
        HttpResponse<byte[]> resposta = clienteHttp.send(requisicao.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new WahaHttpResponse(resposta.statusCode(), resposta.body());
    }
}
