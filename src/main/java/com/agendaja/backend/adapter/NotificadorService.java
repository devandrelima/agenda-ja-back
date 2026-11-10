package com.agendaja.backend.adapter;

import com.agendaja.backend.model.Agendamento;

public interface NotificadorService {
	void enviarSolicitacaoAgendamento(Agendamento agendamento);
	void enviarConfirmacaoAgendamento(Agendamento agendamento);
	void enviarRecuperacaoSenha(String emailDestino, String token);
}