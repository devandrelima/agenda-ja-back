package com.agendaja.backend.controller;

import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.dto.AvaliacaoResumoDTO;
import com.agendaja.backend.service.AvaliacaoPublicaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoPublicaControllerTest {
    @Mock private AvaliacaoPublicaService avaliacaoPublicaService;
    @InjectMocks private AvaliacaoPublicaController controller;

    @Test
    void deveDelegarListagemEResumoDeAvaliacoesPublicas() {
        var lista = List.<AvaliacaoResponseDTO>of();
        var resumo = new AvaliacaoResumoDTO(1L, 5L, new BigDecimal("4.80"));

        when(avaliacaoPublicaService.listar(1L)).thenReturn(lista);
        when(avaliacaoPublicaService.consultarResumo(1L)).thenReturn(resumo);

        assertSame(lista, controller.listar(1L));
        assertSame(resumo, controller.consultarResumo(1L));
    }
}
