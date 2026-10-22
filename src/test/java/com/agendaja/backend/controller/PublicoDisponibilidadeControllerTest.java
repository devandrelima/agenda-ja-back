package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.service.CalculadoraDisponibilidadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicoDisponibilidadeController.class)
@Import(SecurityConfig.class)
class PublicoDisponibilidadeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculadoraDisponibilidadeService disponibilidadeService;

    private static final String BASE_URL = "/api/estabelecimentos/1/servicos/10/disponibilidade";

    @Test
    void consultar_deveRetornarHorariosParaUmProfissional() throws Exception {
        LocalDate data = LocalDate.of(2026, 10, 26);
        List<String> horarios = List.of("08:00", "08:30");

        when(disponibilidadeService.consultarHorarios(1L, 10L, 1L, data))
                .thenReturn(horarios);

        mockMvc.perform(get(BASE_URL)
                .param("data", "2026-10-26")
                .param("profissionalId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("08:00"))
                .andExpect(jsonPath("$[1]").value("08:30"));
    }

    @Test
    void consultar_deveRetornarHorariosParaQualquerProfissionalSeOmitirId() throws Exception {
        LocalDate data = LocalDate.of(2026, 10, 26);
        List<String> horariosMultiplos = List.of("08:00", "09:00", "14:00");

        when(disponibilidadeService.consultarHorarios(1L, 10L, null, data))
                .thenReturn(horariosMultiplos);

        mockMvc.perform(get(BASE_URL)
                .param("data", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("08:00"))
                .andExpect(jsonPath("$[2]").value("14:00"));
    }
}
