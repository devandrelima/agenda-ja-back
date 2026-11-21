package com.agendaja.backend.controller;

import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.service.WahaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/whatsapp")
@RequiredArgsConstructor
public class WahaAdminController {
    private final EstabelecimentoAuthorizationService authorizationService;
    private final WahaService wahaService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public JsonNode status(@PathVariable Long estabelecimentoId, Authentication authentication) {
        authorizationService.validarAcesso(authentication.getName(), estabelecimentoId);
        if (!wahaService.habilitado()) return objectMapper.createObjectNode().put("enabled", false);
        return wahaService.consultarSessao(estabelecimentoId);
    }
    @PostMapping("/conexao")
    public JsonNode iniciar(@PathVariable Long estabelecimentoId, Authentication authentication) {
        authorizationService.validarAcesso(authentication.getName(), estabelecimentoId);
        return wahaService.iniciarSessao(estabelecimentoId);
    }
    @GetMapping("/qr")
    public JsonNode qr(@PathVariable Long estabelecimentoId, Authentication authentication) {
        authorizationService.validarAcesso(authentication.getName(), estabelecimentoId);
        return wahaService.consultarQr(estabelecimentoId);
    }
}
