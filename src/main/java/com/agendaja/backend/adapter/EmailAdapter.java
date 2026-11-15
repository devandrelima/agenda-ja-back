package com.agendaja.backend.adapter;

import com.agendaja.backend.model.Agendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

@Component
public class EmailAdapter implements NotificadorService {
	private static final Logger logger = LoggerFactory.getLogger(EmailAdapter.class);
	private final JavaMailSender mailSender;

	@Value("${app.mail.from:}")
    private String remetente;

	public EmailAdapter(JavaMailSender mailSender)
	{
		this.mailSender = mailSender;
	}

	@Override
	public void enviarSolicitacaoAgendamento(Agendamento agendamento)
	{
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(agendamento.getCliente().getEmail());
			message.setSubject("Solicitação de Agendamento Recebida - AgendaJá");

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			String texto = String.format(
					"Olá %s,\n\n" +
					"Recebemos a sua solicitação de agendamento para o serviço '%s' com o profissional %s, para a data %s.\n\n" +
					"O seu agendamento está atualmente com o status AGUARDANDO CONFIRMAÇÃO. Por favor, acesse o sistema para confirmar a sua presença.\n\n" +
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
	public void enviarConfirmacaoAgendamento(Agendamento agendamento)
	{
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(agendamento.getCliente().getEmail());
			message.setSubject("Confirmação de Agendamento - AgendaJá");

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			String texto = String.format(
					"Olá %s,\n\n" +
					"Seu agendamento para o serviço '%s' com o profissional %s foi CONFIRMADO para %s.\n\n" +
					"Aguardamos você!\n\n" +
					"Atenciosamente,\n" +
					"Equipe AgendaJá",
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
	public void enviarRecuperacaoSenha(String emailDestino, String token)
	{
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(emailDestino);
			message.setSubject("Recuperação de Senha - AgendaJá");
			message.setText(
				"Você solicitou a recuperação de sua senha no AgendaJá.\n\n" +
				"Utilize o token a seguir para redefinir sua senha: " + token + "\n\n" +
				"Caso não tenha solicitado, apenas ignore este e-mail."
			);

			mailSender.send(message);
			logger.info("E-mail de recuperação de senha enviado com sucesso para: {}", emailDestino);
		} catch (Exception e) {
			logger.error("Falha ao enviar e-mail de recuperação de senha para: {}. Erro: {}", emailDestino, e.getMessage());
		}
	}

	@Override
    public void enviarLembreteAgendamento(Agendamento agendamento)
	{
        String emailDestino = agendamento.getCliente().getEmail();
        String nomeCliente = agendamento.getCliente().getNome();
        String servico = agendamento.getServico().getNome();
        String data = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
        String estabelecimento = agendamento.getProfissional().getEstabelecimento().getNome();
        String local = agendamento.getProfissional().getEstabelecimento().getLocalizacao();

        String assunto = "Lembrete de Agendamento Amanhã - AgendaJá";
        
        // Template com os requisitos Data, Serviço e Local
        String corpo = String.format("""
                Olá, %s!
                
                Este é um lembrete automático de que você tem um atendimento agendado para amanhã:
                
                Detalhes da Reserva:
                - Serviço: %s
                - Data e Hora: %s às %s
                - Local: %s (%s)
                
                Para reagendar ou cancelar, acesse a plataforma com antecedência.
                
                Até breve!
                Equipe AgendaJá
                """, nomeCliente, servico, data, hora, estabelecimento, local);

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(emailDestino);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);

            if (remetente != null && !remetente.isBlank()) mensagem.setFrom(remetente);

            mailSender.send(mensagem);
            logger.info("Lembrete de agendamento enviado para o cliente {}.", emailDestino);
        } catch (Exception e) {
            logger.error("Falha ao enviar e-mail de lembrete para o agendamento ID: {}. Erro: {}", agendamento.getId(), e.getMessage());
        }
    }
}
