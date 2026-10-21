package com.agendaja.backend.controller;

import com.agendaja.backend.config.SecurityConfig;
import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.dto.ProfissionalResponseDTO;
import com.agendaja.backend.service.ProfissionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfissionalController.class)
@Import(SecurityConfig.class)
class ProfissionalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfissionalService profissionalService;

    private static final String BASE_URL = "/api/admin/estabelecimentos/1/profissionais";

    private final String dtoValidoJson = """
            {
              "nome": "Lucas Mendes",
              "idsServicos": [1, 2],
              "diasDeAtendimento": [1, 2, 3, 4, 5],
              "diasDeFolga": ["2026-10-15"]
            }
            """;

    @Test
    void criar_deveRetornar201EChamarService() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas Mendes", List.of("Corte", "Barba"), List.of(1L, 2L), List.of(1, 2, 3, 4, 5), List.of("2026-10-15"), 1L);

        when(profissionalService.criar(eq(1L), any(ProfissionalRequestDTO.class), eq("gestor@email.com")))
                .thenReturn(responseDTO);

        mockMvc.perform(post(BASE_URL)
                .with(user("gestor@email.com").roles("GESTOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(dtoValidoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Lucas Mendes"));

        verify(profissionalService).criar(eq(1L), any(ProfissionalRequestDTO.class), eq("gestor@email.com"));
    }

    @Test
    void criar_deveRejeitarSeFaltarCampos() throws Exception {
        var jsonInvalido = "{}";

        mockMvc.perform(post(BASE_URL)
                .with(user("gestor@email.com").roles("GESTOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.idsServicos").exists())
                .andExpect(jsonPath("$.erros.diasDeAtendimento").exists())
                .andExpect(jsonPath("$.erros.diasDeFolga").exists());
    }

    @Test
    void listar_deveRetornarListaDeProfissionais() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas Mendes", List.of("Corte", "Barba"), List.of(1L, 2L), List.of(1, 2, 3, 4, 5), List.of("2026-10-15"), 1L);

        when(profissionalService.listar(1L, "gestor@email.com")).thenReturn(List.of(responseDTO));

        mockMvc.perform(get(BASE_URL)
                .with(user("gestor@email.com").roles("GESTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Lucas Mendes"));
    }

    @Test
    void consultar_deveRetornarProfissional() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas Mendes", List.of("Corte", "Barba"), List.of(1L, 2L), List.of(1, 2, 3, 4, 5), List.of("2026-10-15"), 1L);

        when(profissionalService.consultar(1L, 1L, "gestor@email.com")).thenReturn(responseDTO);

        mockMvc.perform(get(BASE_URL + "/1")
                .with(user("gestor@email.com").roles("GESTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void editar_deveRetornarProfissionalEditado() throws Exception {
        var responseDTO = new ProfissionalResponseDTO(1L, "Lucas Mendes", List.of("Corte", "Barba"), List.of(1L, 2L), List.of(1, 2, 3, 4, 5), List.of("2026-10-15"), 1L);

        when(profissionalService.editar(eq(1L), eq(1L), any(ProfissionalRequestDTO.class), eq("gestor@email.com")))
                .thenReturn(responseDTO);

        mockMvc.perform(put(BASE_URL + "/1")
                .with(user("gestor@email.com").roles("GESTOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(dtoValidoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
