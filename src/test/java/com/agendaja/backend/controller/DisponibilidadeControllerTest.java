package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.DisponibilidadeRequestDTO;
import com.agendaja.backend.dto.DisponibilidadeResponseDTO;
import com.agendaja.backend.dto.FolgaDTO;
import com.agendaja.backend.dto.JornadaTrabalhoDTO;
import com.agendaja.backend.service.DisponibilidadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisponibilidadeController.class)
@Import(SecurityConfig.class)
class DisponibilidadeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisponibilidadeService disponibilidadeService;

    private static final String BASE_URL = "/api/admin/estabelecimentos/1/profissionais/1/disponibilidade";

    @Test
    void consultar_deveRetornarDisponibilidade() throws Exception
    {
        var responseDTO = new DisponibilidadeResponseDTO(
                1L,
                List.of(new JornadaTrabalhoDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0))),
                List.of(new FolgaDTO(LocalDate.of(2026, 12, 25), "Natal"))
        );

        when(disponibilidadeService.consultar(1L, 1L, "gestor@email.com")).thenReturn(responseDTO);

        mockMvc.perform(get(BASE_URL)
                .with(user("gestor@email.com").roles("GESTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissionalId").value(1))
                .andExpect(jsonPath("$.jornadas[0].diaSemana").value(1))
                .andExpect(jsonPath("$.folgas[0].motivo").value("Natal"));
    }

    @Test
    void atualizar_deveAtualizarDisponibilidade() throws Exception
    {
        var responseDTO = new DisponibilidadeResponseDTO(
                1L,
                List.of(new JornadaTrabalhoDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0))),
                List.of(new FolgaDTO(LocalDate.of(2026, 12, 25), "Natal"))
        );

        when(disponibilidadeService.atualizar(eq(1L), eq(1L), any(DisponibilidadeRequestDTO.class), eq("gestor@email.com")))
                .thenReturn(responseDTO);

        String jsonRequest = """
                {
                  "jornadas": [
                    { "diaSemana": 1, "horaInicio": "08:00:00", "horaFim": "12:00:00" }
                  ],
                  "folgas": [
                    { "data": "2026-12-25", "motivo": "Natal" }
                  ]
                }
                """;

        mockMvc.perform(put(BASE_URL)
                .with(user("gestor@email.com").roles("GESTOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissionalId").value(1));
    }
}
