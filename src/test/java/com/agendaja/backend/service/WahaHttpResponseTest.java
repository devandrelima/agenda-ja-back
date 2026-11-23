package com.agendaja.backend.service;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WahaHttpResponseTest {
    @Test
    void deveConverterCorpoNuloParaArrayVazio() {
        var resposta = new WahaHttpResponse(204, (String) null);

        assertEquals(204, resposta.statusCode());
        assertEquals(0, resposta.body().length);
        assertEquals("", resposta.bodyAsString());
    }

    @Test
    void deveConverterCorpoTextoParaBytesUtf8() {
        var resposta = new WahaHttpResponse(200, "Olá, AgendaJá!");

        assertEquals(200, resposta.statusCode());
        assertArrayEquals("Olá, AgendaJá!".getBytes(StandardCharsets.UTF_8), resposta.body());
        assertEquals("Olá, AgendaJá!", resposta.bodyAsString());
    }

    @Test
    void devePreservarBytesNoConstrutorCanonico() {
        byte[] binario = new byte[] {1, 2, 3};
        var resposta = new WahaHttpResponse(201, binario);

        assertEquals(201, resposta.statusCode());
        assertArrayEquals(binario, resposta.body());
    }
}
