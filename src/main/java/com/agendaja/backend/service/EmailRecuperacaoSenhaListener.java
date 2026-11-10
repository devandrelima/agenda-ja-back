package com.agendaja.backend.service;

import com.agendaja.backend.adapter.NotificadorService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailRecuperacaoSenhaListener {

	private final NotificadorService notificadorService;

	public EmailRecuperacaoSenhaListener(NotificadorService notificadorService) {
		this.notificadorService = notificadorService;
	}

	@Async
	@EventListener
	public void onEmailRecuperacaoSenhaEvent(EmailRecuperacaoSenhaEvent event) {
		notificadorService.enviarRecuperacaoSenha(event.destinatario(), event.link());
	}
}