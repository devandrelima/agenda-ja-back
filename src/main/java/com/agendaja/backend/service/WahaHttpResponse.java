package com.agendaja.backend.service;

import java.nio.charset.StandardCharsets;

public record WahaHttpResponse(int statusCode, byte[] body) {
    public WahaHttpResponse(int statusCode, String body) {
        this(statusCode, body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
