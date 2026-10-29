package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.AgendamentoDetalhesDTO;
import com.agendaja.backend.service.AgendamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgendamentoController.class)
@Import(SecurityConfig.class)
class AgendamentoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendamentoService agendamentoService;

    private static final String BASE_URL = "/api/estabelecimentos/1/agendamentos/100";

    @Test
    void consultar_deveRetornarDetalhesParaUsuarioAutorizado() throws Exception {
        var dto = new AgendamentoDetalhesDTO(
                100L,
                "Salão Aurora",
                "Corte",
                "João",
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 10, 30),
                "CONFIRMADO",
                new BigDecimal("50.00")
        );

        when(agendamentoService.consultarDetalhes(1L, 100L, "cliente@teste.com")).thenReturn(dto);

        mockMvc.perform(get(BASE_URL)
                .with(user("cliente@teste.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estabelecimentoNome").value("Salão Aurora"))
                .andExpect(jsonPath("$.servicoNome").value("Corte"))
                .andExpect(jsonPath("$.profissionalNome").value("João"))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.valor").value(50.00));
    }
}
