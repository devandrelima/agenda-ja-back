package com.agendaja.backend.adapter;

import com.agendaja.backend.model.Agendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class EmailAdapter implements NotificadorService {

	private static final Logger logger = LoggerFactory.getLogger(EmailAdapter.class);
	private final JavaMailSender mailSender;

	public EmailAdapter(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void enviarSolicitacaoAgendamento(Agendamento agendamento) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(agendamento.getCliente().getEmail());
			message.setSubject("Solicitação de Agendamento Recebida - AgendaJá");

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			String texto = String.format(
					"Olá %s,\n\nRecebemos a sua solicitação de agendamento para o serviço '%s' com o profissional %s, para a data %s.\n\n" +
							"O seu agendamento está atualmente com o status AGUARDANDO CONFIRMAÇÃO. " +
							"Por favor, acesse o sistema para confirmar a sua presença.\n\n" +
							"Atenciosamente,\nEquipe AgendaJá",
					agendamento.getCliente().getNome(),
					agendamento.getServico().getNome(),
					agendamento.getProfissional().getNome(),
					agendamento.getDataHoraInicio().format(formatter)
			);

			message.setText(texto);
			mailSender.send(message);

			logger.info("E-mail de solicitação recebida enviado com sucesso para o agendamento ID: {}", agendamento.getId());
		} catch (Exception e) {
			logger.error("Falha ao enviar e-mail de solicitação para o agendamento ID: {}. Erro: {}", agendamento.getId(), e.getMessage());
		}
	}

	@Override
	public void enviarConfirmacaoAgendamento(Agendamento agendamento) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(agendamento.getCliente().getEmail());
			message.setSubject("Confirmação de Agendamento - AgendaJá");

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			String texto = String.format(
					"Olá %s,\n\nSeu agendamento para o serviço '%s' com o profissional %s foi CONFIRMADO para %s.\n\n" +
							"Aguardamos você!\n\nAtenciosamente,\nEquipe AgendaJá",
					agendamento.getCliente().getNome(),
					agendamento.getServico().getNome(),
					agendamento.getProfissional().getNome(),
					agendamento.getDataHoraInicio().format(formatter)
			);

			message.setText(texto);
			mailSender.send(message);

			logger.info("E-mail de confirmação enviado com sucesso para o agendamento ID: {}", agendamento.getId());

		} catch (Exception e) {
			logger.error("Falha ao enviar e-mail de confirmação para o agendamento ID: {}. Erro: {}", agendamento.getId(), e.getMessage());
		}
	}

	@Override
	public void enviarRecuperacaoSenha(String emailDestino, String token) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(emailDestino);
			message.setSubject("Recuperação de Senha - AgendaJá");
			message.setText("Você solicitou a recuperação de sua senha no AgendaJá.\n\n" +
					"Utilize o token a seguir para redefinir sua senha: " + token + "\n\n" +
					"Caso não tenha solicitado, apenas ignore este e-mail.");

			mailSender.send(message);
			logger.info("E-mail de recuperação de senha enviado com sucesso para: {}", emailDestino);
		} catch (Exception e) {
			logger.error("Falha ao enviar e-mail de recuperação de senha para: {}. Erro: {}", emailDestino, e.getMessage());
		}
	}
}