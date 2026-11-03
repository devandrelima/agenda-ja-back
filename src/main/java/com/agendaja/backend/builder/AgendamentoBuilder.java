package com.agendaja.backend.builder;

import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

public class AgendamentoBuilder {

    private final Agendamento agendamento;

    public AgendamentoBuilder() {
        this.agendamento = new Agendamento();

        this.agendamento.setStatus(
                StatusAgendamento.AGUARDANDO_CONFIRMACAO
        );
    }

    public AgendamentoBuilder comCliente(Usuario cliente) {
        this.agendamento.setCliente(cliente);
        return this;
    }

    public AgendamentoBuilder comProfissional(
            Profissional profissional
    ) {
        this.agendamento.setProfissional(profissional);
        return this;
    }

    public AgendamentoBuilder comServico(Servico servico) {
        this.agendamento.setServico(servico);
        return this;
    }

    public AgendamentoBuilder comDataHoraInicio(
            LocalDateTime inicio
    ) {
        this.agendamento.setDataHoraInicio(inicio);
        return this;
    }

    public AgendamentoBuilder comObservacoes(
            String observacoes
    ) {
        this.agendamento.setObservacoes(observacoes);
        return this;
    }

    public Agendamento build() {
        // Verifica os campos essenciais.
        if (agendamento.getCliente() == null
                || agendamento.getProfissional() == null
                || agendamento.getServico() == null
                || agendamento.getDataHoraInicio() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dados incompletos para montar o agendamento."
            );
        }

        // Verifica se o profissional realiza o serviço.
        if (!agendamento.getProfissional()
                .getServicos()
                .contains(agendamento.getServico())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O profissional selecionado não realiza este serviço."
            );
        }

        // Calcula o horário final automaticamente.
        LocalDateTime fim = agendamento
                .getDataHoraInicio()
                .plusMinutes(
                        agendamento.getServico()
                                .getDuracaoMinutos()
                );

        agendamento.setDataHoraFim(fim);

        return agendamento;
    }
}
