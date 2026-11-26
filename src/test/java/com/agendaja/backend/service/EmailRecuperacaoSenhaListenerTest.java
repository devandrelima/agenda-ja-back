package com.agendaja.backend.service;

import com.agendaja.backend.adapter.NotificadorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailRecuperacaoSenhaListenerTest {
	@Mock
	private NotificadorService notificadorService;

	@InjectMocks
	private EmailRecuperacaoSenhaListener listener;

	@Test
	void deveDelegarEnvioDoEmailDeRecuperacaoDeSenhaParaONotificador() {
		var evento = new EmailRecuperacaoSenhaEvent(
				"cliente@teste.com",
				"http://localhost:5173/redefinir-senha?token=abc123"
		);

		listener.onEmailRecuperacaoSenhaEvent(evento);

		verify(notificadorService).enviarRecuperacaoSenha(
				"cliente@teste.com",
				"http://localhost:5173/redefinir-senha?token=abc123"
		);
	}
}
