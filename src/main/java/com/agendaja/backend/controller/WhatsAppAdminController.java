package com.agendaja.backend.controller;

import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.service.WahaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoints específicos do painel; eles não expõem a API do WAHA ao navegador. */
@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/whatsapp")
@RequiredArgsConstructor
public class WhatsAppAdminController {
    private final EstabelecimentoAuthorizationService authorizationService;
    private final WahaService wahaService;

    @GetMapping
    public Map<String, String> status(@PathVariable Long estabelecimentoId, Authentication authentication) {
        authorizationService.validarAcesso(authentication.getName(), estabelecimentoId);
        return Map.of("status", wahaService.consultarStatusDaSessao());
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable Long estabelecimentoId, Authentication authentication) {
        authorizationService.validarAcesso(authentication.getName(), estabelecimentoId);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(wahaService.consultarQrCode());
    }
}
