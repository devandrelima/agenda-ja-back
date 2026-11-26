package com.agendaja.backend.controller;

import com.agendaja.backend.dto.EstabelecimentoPublicoDTO;
import com.agendaja.backend.dto.EstabelecimentoResumoPublicoDTO;
import com.agendaja.backend.service.EstabelecimentoPublicoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoPublicoControllerTest {
	@Mock private EstabelecimentoPublicoService service;
	@InjectMocks private EstabelecimentoPublicoController controller;

	@Test
	void deveDelegarListagemEConsultaPublicaDeEstabelecimentos() {
		var publicId = UUID.randomUUID();
		var resumo = mock(EstabelecimentoResumoPublicoDTO.class);
		var lista = List.of(resumo);
		var detalhes = mock(EstabelecimentoPublicoDTO.class);

		when(service.listar()).thenReturn(lista);
		when(service.consultar(publicId)).thenReturn(detalhes);

		assertSame(lista, controller.listar());
		assertSame(detalhes, controller.consultar(publicId));

		verify(service).listar();
		verify(service).consultar(publicId);
	}
}
