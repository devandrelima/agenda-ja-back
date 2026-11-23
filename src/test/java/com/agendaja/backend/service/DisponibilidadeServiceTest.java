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

    @Test
    void deveConsultarDisponibilidadeELancar404QuandoProfissionalNaoExiste() {
        var profissional = new Profissional();
        profissional.setId(2L);
        profissional.setJornadasTrabalho(new java.util.ArrayList<>());
        profissional.setFolgas(new java.util.ArrayList<>());

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(2L, 1L))
                .thenReturn(Optional.of(profissional));
        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(99L, 1L))
                .thenReturn(Optional.empty());

        var resposta = disponibilidadeService.consultar(1L, 2L, "gestor@teste.com");
        assertNotNull(resposta);

        var erro = assertThrows(ResponseStatusException.class,
                () -> disponibilidadeService.consultar(1L, 99L, "gestor@teste.com"));
        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void deveAceitarJornadasEmDiasDiferentesENoMesmoDiaForaDeOrdemSemSobreposicao() {
        var profissional = new Profissional();
        profissional.setId(2L);
        profissional.setJornadasTrabalho(new java.util.ArrayList<>());
        profissional.setFolgas(new java.util.ArrayList<>());

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(2L, 1L))
                .thenReturn(Optional.of(profissional));

        // 14:00-18:00 antes de 08:00-12:00 no dia 1 cobre o curto-circuito false à esquerda do &&;
        // dia 2 cobre diaSemana diferente (false); folga preenchida cobre o laço de folgas.
        var jornadas = List.of(
                new JornadaTrabalhoDTO(1, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                new JornadaTrabalhoDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new JornadaTrabalhoDTO(1, LocalTime.of(17, 0), LocalTime.of(19, 0)),
                new JornadaTrabalhoDTO(2, LocalTime.of(9, 0), LocalTime.of(12, 0))
        );
        var folgas = List.of(new FolgaDTO(LocalDate.of(2026, 12, 25), "Natal"));

        disponibilidadeService.atualizar(1L, 2L, new DisponibilidadeRequestDTO(jornadas, folgas), "gestor@teste.com");

        assertEquals(4, profissional.getJornadasTrabalho().size());
        assertEquals(1, profissional.getFolgas().size());
    }

    @Test
    void devePermitirAtualizarComListasVaziasDeJornadasEFolgas() {
        var profissional = new Profissional();
        profissional.setId(2L);
        profissional.setJornadasTrabalho(new java.util.ArrayList<>());
        profissional.setFolgas(new java.util.ArrayList<>());

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(2L, 1L))
                .thenReturn(Optional.of(profissional));

        disponibilidadeService.atualizar(1L, 2L, new DisponibilidadeRequestDTO(List.of(), List.of()), "gestor@teste.com");

        assertTrue(profissional.getJornadasTrabalho().isEmpty());
        assertTrue(profissional.getFolgas().isEmpty());
    }
}
