package com.agendaja.backend.controller;

import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.agendaja.backend.service.WahaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppAdminControllerTest {
	@Mock private EstabelecimentoAuthorizationService authorizationService;
	@Mock private WahaService wahaService;
	@InjectMocks private WhatsAppAdminController controller;

	private Authentication auth;

	@BeforeEach
	void setup() {
		auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("gestor@teste.com");
	}

	@Test
	void deveValidarAcessoERetornarStatusDaSessaoWhatsApp() {
		when(wahaService.consultarStatusDaSessao()).thenReturn("WORKING");

		var resposta = controller.status(1L, auth);

		assertEquals(Map.of("status", "WORKING"), resposta);
		verify(authorizationService).validarAcesso("gestor@teste.com", 1L);
		verify(wahaService).consultarStatusDaSessao();
	}

	@Test
	void deveValidarAcessoERetornarQrCodePngSemCache() {
		byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
		when(wahaService.consultarQrCode()).thenReturn(png);

		var resposta = controller.qrCode(1L, auth);

		assertEquals(HttpStatus.OK, resposta.getStatusCode());
		assertEquals("no-store", resposta.getHeaders().getCacheControl());
		assertEquals(MediaType.IMAGE_PNG, resposta.getHeaders().getContentType());
		assertArrayEquals(png, resposta.getBody());
		verify(authorizationService).validarAcesso("gestor@teste.com", 1L);
		verify(wahaService).consultarQrCode();
	}
}
