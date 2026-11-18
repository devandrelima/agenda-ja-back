package com.agendaja.backend.builder;

import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgendamentoBuilderTest {
    private Usuario cliente;
    private Servico servico;
    private Profissional profissional;
    private LocalDateTime inicio;

    @BeforeEach
    void setup() {
        cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");

        servico = new Servico();
        servico.setId(10L);
        servico.setNome("Corte");
        servico.setDuracaoMinutos(45);

        profissional = new Profissional();
        profissional.setId(20L);
        profissional.setNome("Lucas");
        profissional.setServicos(List.of(servico));

        inicio = LocalDateTime.of(2026, 12, 10, 14, 0);
    }

    @Test
    void deveConstruirAgendamentoValidoCalculandoHorarioFimEStatusInicial() {
        var agendamento = new AgendamentoBuilder()
                .comCliente(cliente)
                .comProfissional(profissional)
                .comServico(servico)
                .comDataHoraInicio(inicio)
                .comObservacoes("Preferência por tesoura")
                .build();

        assertEquals(cliente, agendamento.getCliente());
        assertEquals(profissional, agendamento.getProfissional());
        assertEquals(servico, agendamento.getServico());
        assertEquals(inicio, agendamento.getDataHoraInicio());
        assertEquals(inicio.plusMinutes(45), agendamento.getDataHoraFim());
        assertEquals(StatusAgendamento.AGUARDANDO_CONFIRMACAO, agendamento.getStatus());
        assertEquals("Preferência por tesoura", agendamento.getObservacoes());
    }

    @Test
    void deveRejeitarQuandoClienteForNulo() {
        var builder = new AgendamentoBuilder()
                .comProfissional(profissional)
                .comServico(servico)
                .comDataHoraInicio(inicio);

        var erro = assertThrows(ResponseStatusException.class, builder::build);
        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void deveRejeitarQuandoProfissionalForNulo() {
        var builder = new AgendamentoBuilder()
                .comCliente(cliente)
                .comServico(servico)
                .comDataHoraInicio(inicio);

        var erro = assertThrows(ResponseStatusException.class, builder::build);
        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void deveRejeitarQuandoServicoForNulo() {
        var builder = new AgendamentoBuilder()
                .comCliente(cliente)
                .comProfissional(profissional)
                .comDataHoraInicio(inicio);

        var erro = assertThrows(ResponseStatusException.class, builder::build);
        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void deveRejeitarQuandoDataHoraInicioForNula() {
        var builder = new AgendamentoBuilder()
                .comCliente(cliente)
                .comProfissional(profissional)
                .comServico(servico);

        var erro = assertThrows(ResponseStatusException.class, builder::build);
        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void deveRejeitarQuandoProfissionalNaoRealizaOServico() {
        var outroServico = new Servico();
        outroServico.setId(99L);
        outroServico.setNome("Coloração");
        outroServico.setDuracaoMinutos(60);

        var builder = new AgendamentoBuilder()
                .comCliente(cliente)
                .comProfissional(profissional)
                .comServico(outroServico)
                .comDataHoraInicio(inicio);

        var erro = assertThrows(ResponseStatusException.class, builder::build);
        assertEquals(400, erro.getStatusCode().value());
        assertEquals("O profissional selecionado não realiza este serviço.", erro.getReason());
    }
}
