package com.agendaja.backend.controller;

import com.agendaja.backend.dto.DashboardResumoDTO;
import com.agendaja.backend.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {
    @Mock private DashboardService dashboardService;
    @InjectMocks private DashboardController controller;

    @Test
    void deveDelegarConsultaDoDashboardParaOServico() {
        var data = LocalDate.of(2026, 12, 10);
        var resumo = mock(DashboardResumoDTO.class);
        var auth = mock(Authentication.class);

        when(auth.getName()).thenReturn("gestor@teste.com");
        when(dashboardService.consultarDashboard(1L, data, "gestor@teste.com")).thenReturn(resumo);

        var resultado = controller.consultarDashboard(1L, data, auth);

        assertSame(resumo, resultado);
        verify(dashboardService).consultarDashboard(1L, data, "gestor@teste.com");
    }
}
