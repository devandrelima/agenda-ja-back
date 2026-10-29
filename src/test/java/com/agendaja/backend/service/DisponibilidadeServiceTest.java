package com.agendaja.backend.service;

import com.agendaja.backend.dto.DisponibilidadeRequestDTO;
import com.agendaja.backend.dto.FolgaDTO;
import com.agendaja.backend.dto.JornadaTrabalhoDTO;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {
    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private EstabelecimentoAuthorizationService authorizationService;

    @InjectMocks
    private DisponibilidadeService disponibilidadeService;

    @Test
    void atualizar_deveSubstituirJornadasEFolgas() {
        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setJornadasTrabalho(new ArrayList<>());
        profissional.setFolgas(new ArrayList<>());

        var dto = new DisponibilidadeRequestDTO(
                List.of(new JornadaTrabalhoDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0))),
                List.of(new FolgaDTO(LocalDate.of(2026, 12, 25), "Natal"))
        );

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L)).thenReturn(Optional.of(profissional));

        var response = disponibilidadeService.atualizar(1L, 1L, dto, "gestor@email.com");

        assertNotNull(response);
        assertEquals(1, response.jornadas().size());
        assertEquals(1, response.folgas().size());
        assertEquals("Natal", response.folgas().get(0).motivo());

        verify(authorizationService).validarAcesso("gestor@email.com", 1L);
    }

    @Test
    void atualizar_deveLancarExcecaoSeHoraInicioForAposHoraFim() {
        var profissional = new Profissional();
        var dto = new DisponibilidadeRequestDTO(
                List.of(new JornadaTrabalhoDTO(1, LocalTime.of(14, 0), LocalTime.of(12, 0))), // Inválido
                List.of()
        );

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L)).thenReturn(Optional.of(profissional));

        var exception = assertThrows(ResponseStatusException.class, () -> disponibilidadeService.atualizar(1L, 1L, dto, "gestor@email.com"));
        assertTrue(exception.getMessage().contains("A hora de início deve ser anterior à hora de fim"));
    }

    @Test
    void atualizar_deveLancarExcecaoSeHouverSobreposicaoNoMesmoDia() {
        var profissional = new Profissional();
        var dto = new DisponibilidadeRequestDTO(
                List.of(
                        new JornadaTrabalhoDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new JornadaTrabalhoDTO(1, LocalTime.of(11, 0), LocalTime.of(15, 0)) // Sobrepõe 11:00 às 12:00
                ),
                List.of()
        );

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L)).thenReturn(Optional.of(profissional));

        var exception = assertThrows(ResponseStatusException.class, () -> disponibilidadeService.atualizar(1L, 1L, dto, "gestor@email.com"));
        assertTrue(exception.getMessage().contains("Existem jornadas sobrepostas para o mesmo dia"));
    }
}
