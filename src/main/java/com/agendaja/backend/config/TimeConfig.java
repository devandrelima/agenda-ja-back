package com.agendaja.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    public static final ZoneId ZONA_PADRAO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock()
    {
        return Clock.system(ZONA_PADRAO);
    }
}
