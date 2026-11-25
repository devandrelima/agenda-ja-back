package com.agendaja.backend.job;

import com.agendaja.backend.adapter.NotificadorService;
import com.agendaja.backend.config.TimeConfig;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.service.WahaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LembreteAgendamentoJobTest {
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private NotificadorService notificadorService;
    @Mock private WahaService wahaService;

    @Test
    void deveCalcularAmanhaPeloFusoLocalMesmoApos21h() {
        // 25/11 às 22:30 em São Paulo (já é 26/11 às 01:30 em UTC) -> amanhã deve ser 26/11
        Clock clockNoite = Clock.fixed(
                LocalDateTime.of(2026, 11, 25, 22, 30)
                        .atZone(TimeConfig.ZONA_PADRAO)
                        .toInstant(),
                TimeConfig.ZONA_PADRAO
        );
        var job = new LembreteAgendamentoJob(agendamentoRepository, notificadorService, wahaService, clockNoite);

        LocalDate amanhaEsperado = LocalDate.of(2026, 11, 26);
        when(agendamentoRepository.buscarElegiveisParaLembrete(
                eq(amanhaEsperado.atStartOfDay()),
                eq(amanhaEsperado.atTime(LocalTime.MAX)),
                anyList()
        )).thenReturn(List.of());

        job.processarLembretesDeAmanha();

        verify(agendamentoRepository).buscarElegiveisParaLembrete(
                eq(amanhaEsperado.atStartOfDay()),
                eq(amanhaEsperado.atTime(LocalTime.MAX)),
                eq(List.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.AGUARDANDO_CONFIRMACAO))
        );
        verifyNoInteractions(notificadorService, wahaService);
    }

    @Test
    void deveContinuarProcessamentoQuandoEnvioDeUmAgendamentoFalhar() {
        Clock clockManha = Clock.fixed(
                LocalDateTime.of(2026, 11, 25, 8, 0)
                        .atZone(TimeConfig.ZONA_PADRAO)
                        .toInstant(),
                TimeConfig.ZONA_PADRAO
        );
        var job = new LembreteAgendamentoJob(agendamentoRepository, notificadorService, wahaService, clockManha);

        var ag1 = new Agendamento();
        ag1.setId(1L);
        var ag2 = new Agendamento();
        ag2.setId(2L);

        when(agendamentoRepository.buscarElegiveisParaLembrete(any(), any(), anyList()))
                .thenReturn(List.of(ag1, ag2));
        doThrow(new RuntimeException("falha")).when(notificadorService).enviarLembreteAgendamento(ag1);

        job.processarLembretesDeAmanha();

        assertFalse(ag1.isLembreteEnviado());
        assertTrue(ag2.isLembreteEnviado());
        verify(wahaService, never()).enviarLembrete(ag1);
        verify(wahaService).enviarLembrete(ag2);
    }
}
