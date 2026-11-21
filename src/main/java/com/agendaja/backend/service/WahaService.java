package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WahaService {
    private static final Logger log = LoggerFactory.getLogger(WahaService.class);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private final ObjectMapper objectMapper;
    private final HttpClient clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${app.waha.enabled:false}") private boolean habilitado;
    @Value("${app.waha.base-url:http://agenda-ja-waha:3000}") private String baseUrl;
    @Value("${app.waha.api-key:}") private String apiKey;
    @Value("${app.waha.session-prefix:estabelecimento-}") private String prefixoSessao;

    public boolean habilitado() { return habilitado; }
    public String sessao(Long estabelecimentoId) { return prefixoSessao + estabelecimentoId; }

    public JsonNode consultarSessao(Long estabelecimentoId) { return requisitar("GET", "/api/sessions/" + sessao(estabelecimentoId), null); }
    public JsonNode iniciarSessao(Long estabelecimentoId) { return requisitar("POST", "/api/sessions/" + sessao(estabelecimentoId) + "/start", Map.of()); }
    public JsonNode consultarQr(Long estabelecimentoId) { return requisitar("GET", "/api/" + sessao(estabelecimentoId) + "/auth/qr", null); }

    public void enviarSolicitacao(Agendamento agendamento) {
        enviar(agendamento, "Olá, %s! Recebemos sua solicitação de %s com %s para %s. Aguarde a confirmação do estabelecimento.");
    }
    public void enviarConfirmacao(Agendamento agendamento) {
        enviar(agendamento, "Olá, %s! Seu agendamento de %s com %s foi confirmado para %s.");
    }
    public void enviarReagendamento(Agendamento agendamento) {
        enviar(agendamento, "Olá, %s! Seu agendamento de %s com %s foi reagendado para %s.");
    }
    public void enviarLembrete(Agendamento agendamento) {
        enviar(agendamento, "Olá, %s! Lembrete: você tem %s com %s em %s.");
    }

    private void enviar(Agendamento agendamento, String modelo) {
        if (!habilitado) return;
        try {
            String telefone = normalizarTelefone(agendamento.getCliente().getTelefone());
            String texto = modelo.formatted(agendamento.getCliente().getNome(), agendamento.getServico().getNome(),
                    agendamento.getProfissional().getNome(), agendamento.getDataHoraInicio().format(DATA_HORA));
            requisitar("POST", "/api/sendText", Map.of("session", sessao(agendamento.getProfissional().getEstabelecimento().getId()),
                    "chatId", telefone + "@c.us", "text", texto));
        } catch (Exception ex) {
            log.warn("Não foi possível enviar WhatsApp para o agendamento {}: {}", agendamento.getId(), ex.getMessage());
        }
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitos.length() == 10 || digitos.length() == 11) return "55" + digitos;
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) return digitos;
        throw new IllegalArgumentException("Telefone de cliente inválido.");
    }

    private JsonNode requisitar(String metodo, String caminho, Object corpo) {
        if (!habilitado) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp não está habilitado.");
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp não está configurado.");
        try {
            HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/$", "") + caminho))
                    .timeout(Duration.ofSeconds(10)).header("X-Api-Key", apiKey).header("Accept", "application/json");
            if ("GET".equals(metodo)) requisicao.GET();
            else requisicao.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(corpo)));
            HttpResponse<String> resposta = clienteHttp.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WAHA recusou a solicitação.");
            return resposta.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(resposta.body());
        } catch (ResponseStatusException ex) { throw ex;
        } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WAHA está indisponível."); }
    }
}
