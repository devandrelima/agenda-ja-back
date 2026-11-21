package com.agendaja.backend.service;

import com.agendaja.backend.model.Agendamento;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Adaptador isolado para WAHA. Falhas nele não interrompem agendamentos. */
@Service
@RequiredArgsConstructor
public class WahaService {
    private static final Logger log = LoggerFactory.getLogger(WahaService.class);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ObjectMapper objectMapper;
    private final WahaHttpGateway httpGateway;

    @Value("${app.waha.enabled:false}") private boolean habilitado;
    @Value("${app.waha.base-url:http://agenda-ja-waha:3000}") private String baseUrl;
    @Value("${app.waha.api-key:}") private String apiKey;
    @Value("${app.waha.session:agendaja}") private String sessao;
    @Value("${app.waha.read-timeout-ms:10000}") private long readTimeoutMs;

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

    /** Usado apenas pelo painel autenticado para orientar o pareamento. Não habilita envios. */
    public String consultarStatusDaSessao() {
        WahaHttpResponse resposta = requisitarAdministrativo("GET", "/api/sessions/" + sessao);
        try {
            JsonNode dados = objectMapper.readTree(resposta.bodyAsString());
            return dados.path("status").asText(dados.path("state").asText("UNKNOWN"));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WAHA retornou uma resposta inválida.");
        }
    }

    /** Retorna somente a imagem binária do QR Code; nenhum segredo é entregue ao navegador. */
    public byte[] consultarQrCode() {
        return requisitarAdministrativo("GET", "/api/" + sessao + "/auth/qr").body();
    }

    private void enviar(Agendamento agendamento, String modelo) {
        if (!habilitado) return;
        if (!configurado()) {
            log.warn("Notificação WhatsApp ignorada: configuração WAHA incompleta.");
            return;
        }
        try {
            if (!sessaoEmFuncionamento()) return;

            String telefone;
            try {
                telefone = normalizarTelefone(agendamento.getCliente().getTelefone());
            } catch (IllegalArgumentException ex) {
                log.warn("Notificação WhatsApp ignorada: telefone do destinatário inválido.");
                return;
            }
            String chatId = resolverChatId(telefone);
            if (chatId == null) return;
            String texto = modelo.formatted(agendamento.getCliente().getNome(), agendamento.getServico().getNome(),
                    agendamento.getProfissional().getNome(), agendamento.getDataHoraInicio().format(DATA_HORA));
            String corpo = objectMapper.writeValueAsString(Map.of("session", sessao, "chatId", chatId, "text", texto));
            WahaHttpResponse resposta = httpGateway.executar("POST", uri("/api/sendText"), apiKey, corpo, timeout());
            if (resposta.statusCode() >= 300) registrarErro("envio", resposta.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Notificação WhatsApp interrompida para o agendamento {}.", agendamento.getId());
        } catch (Exception ex) {
            log.warn("WAHA indisponível; notificação ignorada para o agendamento {}.", agendamento.getId());
        }
    }

    /** Resolve o identificador canônico do WAHA (por exemplo, @lid ou @c.us), sem inferi-lo localmente. */
    private String resolverChatId(String telefone) {
        try {
            String consulta = "/api/contacts/check-exists?phone=" + codificar(telefone) + "&session=" + codificar(sessao);
            WahaHttpResponse resposta = httpGateway.executar("GET", uri(consulta), apiKey, null, timeout());
            if (resposta.statusCode() == 401) {
                log.warn("WAHA recusou a credencial durante verificação do destinatário.");
                return null;
            }
            if (resposta.statusCode() >= 300) {
                registrarErroContato(resposta.statusCode());
                return null;
            }
            JsonNode dados = objectMapper.readTree(resposta.bodyAsString());
            if (!dados.has("numberExists") || !dados.path("numberExists").isBoolean()) {
                log.warn("WAHA retornou resposta inválida ao verificar destinatário.");
                return null;
            }
            if (!dados.path("numberExists").asBoolean()) {
                log.info("Notificação WhatsApp ignorada: destinatário não possui WhatsApp.");
                return null;
            }
            String chatId = dados.path("chatId").asText("").strip();
            if (chatId.isEmpty()) {
                log.warn("WAHA retornou destinatário sem chatId.");
                return null;
            }
            return chatId;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Verificação do destinatário WAHA interrompida.");
        } catch (java.net.http.HttpTimeoutException ex) {
            log.warn("Tempo esgotado ao verificar destinatário no WAHA.");
        } catch (Exception ex) {
            log.warn("WAHA indisponível durante verificação do destinatário.");
        }
        return null;
    }

    private boolean sessaoEmFuncionamento() {
        try {
            WahaHttpResponse resposta = httpGateway.executar("GET", uri("/api/sessions/" + sessao), apiKey, null, timeout());
            if (resposta.statusCode() != 200) {
                registrarErro("consulta de sessão", resposta.statusCode());
                return false;
            }
            JsonNode dados = objectMapper.readTree(resposta.bodyAsString());
            String status = dados.path("status").asText(dados.path("state").asText(""));
            if (!"WORKING".equalsIgnoreCase(status)) {
                log.info("Notificação WhatsApp ignorada: sessão não está pronta ({}).", status.isBlank() ? "sem estado" : status);
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Consulta da sessão WAHA interrompida.");
        } catch (Exception ex) {
            log.warn("WAHA indisponível; consulta da sessão ignorada.");
        }
        return false;
    }

    private boolean configurado() {
        return !baseUrl.isBlank() && !apiKey.isBlank() && !sessao.isBlank();
    }

    private WahaHttpResponse requisitarAdministrativo(String metodo, String caminho) {
        if (!configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp ainda não está configurado no servidor.");
        }
        try {
            WahaHttpResponse resposta = httpGateway.executar(metodo, uri(caminho), apiKey, null, timeout());
            if (resposta.statusCode() == 401) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A credencial do WhatsApp foi recusada.");
            }
            if (resposta.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão do WhatsApp não encontrada.");
            }
            if (resposta.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WAHA está indisponível.");
            }
            return resposta;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WAHA está indisponível.");
        }
    }

    private URI uri(String caminho) {
        return URI.create(baseUrl.replaceAll("/$", "") + caminho);
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private Duration timeout() {
        return Duration.ofMillis(readTimeoutMs);
    }

    private void registrarErro(String operacao, int status) {
        if (status == 401) log.warn("WAHA recusou a credencial durante {}.", operacao);
        else if (status == 404) log.info("Sessão WAHA não encontrada; notificação ignorada.");
        else if (status >= 500) log.warn("WAHA indisponível durante {}.", operacao);
        else log.warn("WAHA recusou {} (HTTP {}).", operacao, status);
    }

    private void registrarErroContato(int status) {
        if (status == 404) log.warn("WAHA não encontrou o endpoint de verificação do destinatário.");
        else if (status >= 500) log.warn("WAHA indisponível durante verificação do destinatário.");
        else log.warn("WAHA recusou a verificação do destinatário (HTTP {}).", status);
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitos.length() == 10 || digitos.length() == 11) return "55" + digitos;
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) return digitos;
        throw new IllegalArgumentException("Telefone de cliente inválido.");
    }
}
