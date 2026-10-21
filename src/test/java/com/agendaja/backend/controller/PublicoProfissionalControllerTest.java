package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.ProfissionalResponseDTO;
import com.agendaja.backend.service.ProfissionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicoProfissionalController.class)
@Import(SecurityConfig.class)
class PublicoProfissionalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfissionalService profissionalService;

    private static final String BASE_URL = "/api/estabelecimentos/1/servicos/10/profissionais";

    @Test
    void listarPorServico_deveRetornarListaDeProfissionaisPublicamente() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas", List.of("Corte"), List.of(10L), List.of(1, 2), List.of(), 1L);

        when(profissionalService.listarPorServicoPublico(1L, 10L)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Lucas"));
    }

    @Test
    void sortearPorServico_deveRetornarUmProfissionalAleatorio() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas", List.of("Corte"), List.of(10L), List.of(1, 2), List.of(), 1L);

        when(profissionalService.sortearProfissionalPorServico(1L, 10L)).thenReturn(responseDTO);

        mockMvc.perform(get(BASE_URL + "/aleatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Lucas"));
    }
}
