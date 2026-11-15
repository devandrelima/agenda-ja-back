package com.agendaja.backend.repository;

import java.time.LocalDateTime;

public interface ClienteResumoProjection {
    Long getClienteId();
    String getNome();
    String getTelefone();
    Integer getTotalVisitas();
    LocalDateTime getUltimaVisita();
}
