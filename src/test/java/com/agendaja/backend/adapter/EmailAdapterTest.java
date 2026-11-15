package com.agendaja.backend.adapter;

import com.agendaja.backend.model.Agendamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDateTime;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailAdapterTest {
	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private EmailAdapter emailAdapter;

	private Agendamento agendamentoMock;

	@BeforeEach
	void setUp() {
		agendamentoMock = mock(Agendamento.class, RETURNS_DEEP_STUBS);

		when(agendamentoMock.getCliente().getEmail()).thenReturn("cliente@teste.com");
		when(agendamentoMock.getCliente().getNome()).thenReturn("João Silva");
		when(agendamentoMock.getServico().getNome()).thenReturn("Corte de Cabelo");
		when(agendamentoMock.getProfissional().getNome()).thenReturn("Maria");
		when(agendamentoMock.getDataHoraInicio()).thenReturn(LocalDateTime.of(2026, 12, 1, 14, 0));
	}

	@Test
	void deveEnviarSolicitacaoAgendamentoComSucesso() {
		emailAdapter.enviarSolicitacaoAgendamento(agendamentoMock);

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(1)).send(captor.capture());

		SimpleMailMessage mensagemEnviada = captor.getValue();
		assertEquals("cliente@teste.com", mensagemEnviada.getTo()[0]);
		assertEquals("Solicitação de Agendamento Recebida - AgendaJá", mensagemEnviada.getSubject());
		assertTrue(mensagemEnviada.getText().contains("AGUARDANDO CONFIRMAÇÃO"));
	}

	@Test
	void deveEnviarConfirmacaoAgendamentoComSucesso() {
		emailAdapter.enviarConfirmacaoAgendamento(agendamentoMock);

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(1)).send(captor.capture());

		SimpleMailMessage mensagemEnviada = captor.getValue();
		assertEquals("cliente@teste.com", mensagemEnviada.getTo()[0]);
		assertEquals("Confirmação de Agendamento - AgendaJá", mensagemEnviada.getSubject());
		assertTrue(mensagemEnviada.getText().contains("CONFIRMADO"));
	}

	@Test
	void deveTratarFalhaNoEnvioSemLancarExcecao() {
		doThrow(new RuntimeException("Falha SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

		emailAdapter.enviarConfirmacaoAgendamento(agendamentoMock);

		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
    void deveEnviarLembreteAgendamentoComSucesso() {
        var estabelecimentoMock = mock(com.agendaja.backend.model.Estabelecimento.class);
        when(estabelecimentoMock.getNome()).thenReturn("Salão Aurora");
        when(estabelecimentoMock.getLocalizacao()).thenReturn("Centro");
        when(agendamentoMock.getProfissional().getEstabelecimento()).thenReturn(estabelecimentoMock);

        emailAdapter.enviarLembreteAgendamento(agendamentoMock);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage mensagemEnviada = captor.getValue();
        assertEquals("cliente@teste.com", mensagemEnviada.getTo()[0]);
        assertEquals("Lembrete de Agendamento Amanhã - AgendaJá", mensagemEnviada.getSubject());
        assertTrue(mensagemEnviada.getText().contains("Corte de Cabelo"));
        assertTrue(mensagemEnviada.getText().contains("Salão Aurora"));
    }
}
