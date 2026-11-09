package com.agendaja.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailRecuperacaoSenhaListener {

	private static final Logger log =
			LoggerFactory.getLogger(
					EmailRecuperacaoSenhaListener.class
			);

	private final JavaMailSender mailSender;

	@Value("${app.mail.from}")
	private String remetente;

	@TransactionalEventListener(
			phase = TransactionPhase.AFTER_COMMIT
	)
	public void enviar(EmailRecuperacaoSenhaEvent evento) {
		try {
			MimeMessage mensagem =
					mailSender.createMimeMessage();

			MimeMessageHelper helper =
					new MimeMessageHelper(
							mensagem,
							"UTF-8"
					);

			helper.setFrom(remetente);
			helper.setTo(evento.destinatario());

			helper.setSubject(
					"AgendaJá — recuperação de senha"
			);

			helper.setText("""
                    Você solicitou uma nova senha para o AgendaJá.

                    Acesse o link abaixo para redefini-la.
                    O link é válido por 30 minutos:

                    %s

                    Se você não solicitou esta alteração,
                    ignore este e-mail.
                    """.formatted(evento.link()), false);

			mailSender.send(mensagem);

		} catch (Exception ex) {
			// Nunca registrar o token ou o link nos logs.
			log.error(
					"Falha ao enviar e-mail de recuperação ({})",
					ex.getClass().getSimpleName()
			);
		}
	}
}
