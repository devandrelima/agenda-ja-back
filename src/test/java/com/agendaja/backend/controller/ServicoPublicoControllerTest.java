package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.service.ServicoPublicoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoPublicoControllerTest {
    @Mock private ServicoPublicoService servicoPublicoService;
    @InjectMocks private ServicoPublicoController controller;

    @Test
    void deveDelegarListagemEConsultaPublicaDeServicos() {
        var resp = mock(ServicoResponseDTO.class);
        var lista = List.of(resp);

        when(servicoPublicoService.listar(1L)).thenReturn(lista);
        when(servicoPublicoService.consultar(1L, 10L)).thenReturn(resp);

        assertSame(lista, controller.listar(1L));
        assertSame(resp, controller.consultar(1L, 10L));
    }
}
