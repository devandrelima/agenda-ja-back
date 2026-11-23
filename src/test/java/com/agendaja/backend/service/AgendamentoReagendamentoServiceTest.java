package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoReagendamentoRequestDTO;
import com.agendaja.backend.dto.AgendamentoRequestDTO;
import com.agendaja.backend.dto.AgendamentoResponseDTO;
import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoReagendamentoServiceTest {
	private static final Long CLIENTE_ID = 10L;
	private static final Long ESTABELECIMENTO_ID = 1L;
	private static final Long AGENDAMENTO_ID = 20L;
	private static final Long NOVO_AGENDAMENTO_ID = 21L;
	private static final String EMAIL = "cliente@teste.com";

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private CalculadoraDisponibilidadeService disponibilidadeService;

	@Mock
	private AgendamentoService agendamentoService;

	@InjectMocks
	private AgendamentoReagendamentoService service;

	private Usuario cliente;
	private Agendamento original;
	private Agendamento novo;
	private AgendamentoReagendamentoRequestDTO dto;

	@BeforeEach
	void configurar() {
		cliente = new Usuario(
				CLIENTE_ID,
				null,
				"Maria Silva",
				EMAIL,
				"hash",
				"CLIENTE"
		);

		var estabelecimento = new Estabelecimento(
				ESTABELECIMENTO_ID,
				"Barbearia Central",
				"BARBEARIA",
				null,
				"Mossoró/RN"
		);

		estabelecimento.setPublicId(UUID.randomUUID());

		var servico = new Servico();
		servico.setId(2L);
		servico.setNome("Corte");
		servico.setValor(new BigDecimal("35.00"));
		servico.setDuracaoMinutos(30);
		servico.setEstabelecimento(estabelecimento);

		var profissional = new Profissional();
		profissional.setId(3L);
		profissional.setNome("João");
		profissional.setEstabelecimento(estabelecimento);

		original = new Agendamento();
		original.setId(AGENDAMENTO_ID);
		original.setCliente(cliente);
		original.setServico(servico);
		original.setProfissional(profissional);
		original.setStatus(StatusAgendamento.CONFIRMADO);
		original.setDataHoraInicio(
				LocalDateTime.of(2027, 1, 11, 10, 0)
		);
		original.setDataHoraFim(
				LocalDateTime.of(2027, 1, 11, 10, 30)
		);

		novo = new Agendamento();
		novo.setId(NOVO_AGENDAMENTO_ID);
		novo.setCliente(cliente);
		novo.setServico(servico);
		novo.setProfissional(profissional);
		novo.setStatus(
				StatusAgendamento.AGUARDANDO_CONFIRMACAO
		);
		novo.setDataHoraInicio(
				LocalDateTime.of(2027, 1, 12, 14, 0)
		);
		novo.setDataHoraFim(
				LocalDateTime.of(2027, 1, 12, 14, 30)
		);

		dto = new AgendamentoReagendamentoRequestDTO(
				ESTABELECIMENTO_ID,
				2L,
				3L,
				LocalDateTime.of(2027, 1, 12, 14, 0)
		);
	}

	private void prepararClienteAutenticado()
	{
		when(usuarioRepository.findByEmail(EMAIL))
				.thenReturn(Optional.of(cliente));
	}

	private void prepararReservaOriginal()
	{
		when(agendamentoRepository.findByIdAndClienteId(
				AGENDAMENTO_ID,
				CLIENTE_ID
		)).thenReturn(Optional.of(original));
	}

	@Test
	void deveReagendarComSucesso() {
		prepararClienteAutenticado();
		prepararReservaOriginal();

		when(agendamentoRepository.cancelarSePermitido(
				eq(AGENDAMENTO_ID),
				eq(CLIENTE_ID),
				anyList(),
				eq(StatusAgendamento.CANCELADO)
		)).thenReturn(1);

		when(disponibilidadeService.calcularParaUmProfissional(
				eq(ESTABELECIMENTO_ID),
				eq(3L),
				eq(2L),
				eq(dto.dataHoraInicio().toLocalDate())
		)).thenReturn(List.of(LocalTime.of(14, 0)));

		when(agendamentoService.registrar(
				eq(ESTABELECIMENTO_ID),
				any(AgendamentoRequestDTO.class),
				eq(EMAIL)
		)).thenReturn(new AgendamentoResponseDTO(
				NOVO_AGENDAMENTO_ID,
				2L,
				3L,
				novo.getDataHoraInicio(),
				novo.getDataHoraFim(),
				"AGUARDANDO_CONFIRMACAO",
				new BigDecimal("35.00")
		));

		when(agendamentoRepository.findByIdAndClienteId(
				NOVO_AGENDAMENTO_ID,
				CLIENTE_ID
		)).thenReturn(Optional.of(novo));

		var resposta = service.reagendar(
				AGENDAMENTO_ID,
				dto,
				EMAIL
		);

		assertEquals(NOVO_AGENDAMENTO_ID, resposta.id());
		assertEquals("AGUARDANDO_CONFIRMACAO", resposta.status());
		assertEquals(ESTABELECIMENTO_ID, resposta.estabelecimentoId());

		verify(agendamentoRepository).cancelarSePermitido(
				eq(AGENDAMENTO_ID),
				eq(CLIENTE_ID),
				anyList(),
				eq(StatusAgendamento.CANCELADO)
		);

		verify(agendamentoService).registrar(
				eq(ESTABELECIMENTO_ID),
				argThat(request ->
						request.servicoId().equals(2L)
								&& request.profissionalId().equals(3L)
								&& request.dataHoraInicio()
								.equals(dto.dataHoraInicio())
				),
				eq(EMAIL)
		);
	}

	@Test
	void deveRejeitarReagendamentoDeOutroCliente() {
		prepararClienteAutenticado();

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID, dto, EMAIL
				)
		);

		assertEquals(404, erro.getStatusCode().value());

		verify(agendamentoRepository, never())
				.cancelarSePermitido(
						anyLong(),
						anyLong(),
						anyList(),
						any()
				);

		verifyNoInteractions(agendamentoService);
	}

	@Test
	void deveRejeitarReagendamentoDeReservaCancelada() {
		original.setStatus(StatusAgendamento.CANCELADO);

		prepararClienteAutenticado();
		prepararReservaOriginal();

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID, dto, EMAIL
				)
		);

		assertEquals(409, erro.getStatusCode().value());

		verifyNoInteractions(agendamentoService);
	}

	@Test
	void deveRejeitarReagendamentoDeReservaConcluida() {
		original.setStatus(StatusAgendamento.CONCLUIDO);

		prepararClienteAutenticado();
		prepararReservaOriginal();

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID, dto, EMAIL
				)
		);

		assertEquals(409, erro.getStatusCode().value());

		verifyNoInteractions(agendamentoService);
	}

	@Test
	void deveRejeitarNovoHorarioIndisponivel() {
		prepararClienteAutenticado();
		prepararReservaOriginal();

		when(agendamentoRepository.cancelarSePermitido(
				eq(AGENDAMENTO_ID),
				eq(CLIENTE_ID),
				anyList(),
				eq(StatusAgendamento.CANCELADO)
		)).thenReturn(1);

		when(disponibilidadeService.calcularParaUmProfissional(
				eq(ESTABELECIMENTO_ID),
				eq(3L),
				eq(2L),
				eq(dto.dataHoraInicio().toLocalDate())
		)).thenReturn(List.of(LocalTime.of(15, 0)));

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID, dto, EMAIL
				)
		);

		assertEquals(409, erro.getStatusCode().value());

		verifyNoInteractions(agendamentoService);
	}

	@Test
	void deveRejeitarReagendamentoParaDataPassada() {
		var dataPassada = new AgendamentoReagendamentoRequestDTO(
				ESTABELECIMENTO_ID,
				2L,
				3L,
				LocalDateTime.of(2020, 1, 1, 10, 0)
		);

		prepararClienteAutenticado();
		prepararReservaOriginal();

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID,
						dataPassada,
						EMAIL
				)
		);

		assertEquals(400, erro.getStatusCode().value());

		verifyNoInteractions(disponibilidadeService);
		verifyNoInteractions(agendamentoService);
	}

	@Test
	void deveImpedirGestorDeReagendar() {
		cliente.setRole("GESTOR");
		prepararClienteAutenticado();

		var erro = assertThrows(
				ResponseStatusException.class,
				() -> service.reagendar(
						AGENDAMENTO_ID, dto, EMAIL
				)
		);

		assertEquals(403, erro.getStatusCode().value());

		verifyNoInteractions(agendamentoRepository);
		verifyNoInteractions(agendamentoService);
	}

	@Test
    void deveRejeitarQuandoUsuarioNaoExisteOuNaoECliente() {
        var dto = new AgendamentoReagendamentoRequestDTO(
                1L, 2L, 3L, LocalDateTime.now(com.agendaja.backend.config.TimeConfig.ZONA_PADRAO).plusDays(2)
        );

        when(usuarioRepository.findByEmail("ausente@teste.com")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.reagendar(10L, dto, "ausente@teste.com")).getStatusCode().value());

        var gestor = new Usuario(1L, null, "Gestor", "gestor@teste.com", "hash", "GESTOR");
        when(usuarioRepository.findByEmail("gestor@teste.com")).thenReturn(Optional.of(gestor));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.reagendar(10L, dto, "gestor@teste.com")).getStatusCode().value());
    }

    @Test
    void deveDistinguir404E409QuandoCancelamentoAtomicoNaoAlterarLinhas() {
        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        var original = new Agendamento();
        original.setId(10L);
        original.setStatus(StatusAgendamento.CONFIRMADO);

        var dto = new AgendamentoReagendamentoRequestDTO(
                1L, 2L, 3L, LocalDateTime.now(com.agendaja.backend.config.TimeConfig.ZONA_PADRAO).plusDays(2)
        );

        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.cancelarSePermitido(eq(10L), eq(1L), anyList(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(0);

        // Segunda consulta retorna vazio -> 404
        when(agendamentoRepository.findByIdAndClienteId(10L, 1L))
                .thenReturn(Optional.of(original))
                .thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.reagendar(10L, dto, "maria@teste.com")).getStatusCode().value());

        // Segunda consulta encontra reserva (alterada concorrentemente) -> 409
        when(agendamentoRepository.findByIdAndClienteId(10L, 1L))
                .thenReturn(Optional.of(original))
                .thenReturn(Optional.of(original));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.reagendar(10L, dto, "maria@teste.com")).getStatusCode().value());
    }

    @Test
    void deveLancarIllegalStateExceptionQuandoNovaReservaNaoForEncontradaAposCadastro() {
        var cliente = new Usuario(1L, null, "Maria", "maria@teste.com", "hash", "CLIENTE");
        var original = new Agendamento();
        original.setId(10L);
        original.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);

        var futuro = LocalDateTime.now(com.agendaja.backend.config.TimeConfig.ZONA_PADRAO)
                .plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        var dto = new AgendamentoReagendamentoRequestDTO(1L, 2L, 3L, futuro);

        var respostaCriada = mock(com.agendaja.backend.dto.AgendamentoResponseDTO.class);
        when(respostaCriada.id()).thenReturn(200L);

        when(usuarioRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.findByIdAndClienteId(10L, 1L)).thenReturn(Optional.of(original));
        when(agendamentoRepository.cancelarSePermitido(eq(10L), eq(1L), anyList(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(1);
        when(disponibilidadeService.calcularParaUmProfissional(1L, 3L, 2L, futuro.toLocalDate()))
                .thenReturn(List.of(futuro.toLocalTime()));
        when(agendamentoService.registrar(eq(1L), any(AgendamentoRequestDTO.class), eq("maria@teste.com")))
                .thenReturn(respostaCriada);
        when(agendamentoRepository.findByIdAndClienteId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.reagendar(10L, dto, "maria@teste.com"));
    }
}
