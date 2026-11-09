package com.agendaja.backend.service;

public record EmailRecuperacaoSenhaEvent(
		String destinatario,
		String link
) {}