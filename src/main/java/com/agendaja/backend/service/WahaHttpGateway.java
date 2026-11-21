package com.agendaja.backend.service;

import java.net.URI;
import java.time.Duration;

public interface WahaHttpGateway {
    WahaHttpResponse executar(String metodo, URI uri, String apiKey, String corpo, Duration timeout) throws Exception;
}
