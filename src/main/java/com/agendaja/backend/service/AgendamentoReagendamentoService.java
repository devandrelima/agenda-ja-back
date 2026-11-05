package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.dto.AgendamentoReagendamentoRequestDTO;
import com.agendaja.backend.dto.AgendamentoRequestDTO;

import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoReagendamentoService {

	private final AgendamentoRepository agendamentoRepository;
	private final UsuarioRepository usuarioRepository;
	private final CalculadoraDisponibilidadeService disponibilidadeService;
	private final AgendamentoService agendamentoService;

	@Transactional
	public AgendamentoClienteDTO reagendar(
			Long agendamentoId,
			AgendamentoReagendamentoRequestDTO dto,
			String emailAutenticado
	) {
		// 1. Identificar o cliente autenticado.
		var cliente = usuarioRepository.findByEmail(emailAutenticado)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED,
						"Sessão inválida ou expirada."
				));

		if (!"CLIENTE".equals(cliente.getRole())) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Somente clientes podem reagendar suas reservas."
			);
		}

		// 2. Consultar somente a reserva pertencente ao cliente.
		var reservaOriginal = agendamentoRepository
				.findByIdAndClienteId(agendamentoId, cliente.getId())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Agendamento não encontrado."
				));

		var statusOriginal = reservaOriginal.getStatus();

		if (statusOriginal != StatusAgendamento.AGUARDANDO_CONFIRMACAO
				&& statusOriginal != StatusAgendamento.CONFIRMADO) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Este agendamento não pode mais ser reagendado."
			);
		}

		// 3. Não permitir reagendamento para horários passados.
		if (!dto.dataHoraInicio().isAfter(LocalDateTime.now())) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"O novo horário deve estar no futuro."
			);
		}

		// 4. Cancelar primeiro, mas dentro da mesma transação.
		int alterados = agendamentoRepository.cancelarSePermitido(
				agendamentoId,
				cliente.getId(),
				List.of(
						StatusAgendamento.AGUARDANDO_CONFIRMACAO,
						StatusAgendamento.CONFIRMADO
				),
				StatusAgendamento.CANCELADO
		);

		if (alterados == 0) {
			// Distinguir reserva inexistente de status inválido.
			agendamentoRepository
					.findByIdAndClienteId(agendamentoId, cliente.getId())
					.orElseThrow(() -> new ResponseStatusException(
							HttpStatus.NOT_FOUND,
							"Agendamento não encontrado."
					));

			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"O agendamento foi alterado por outra operação."
			);
		}

		// 5. Validar o novo horário pelas regras da HU10.
		var horariosDisponiveis = disponibilidadeService
				.calcularParaUmProfissional(
						dto.estabelecimentoId(),
						dto.profissionalId(),
						dto.servicoId(),
						dto.dataHoraInicio().toLocalDate()
				);

		if (!horariosDisponiveis.contains(
				dto.dataHoraInicio().toLocalTime()
		)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"O novo horário não está disponível."
			);
		}

		// 6. Reutilizar a criação da HU12.
		var novoAgendamento = agendamentoService.registrar(
				dto.estabelecimentoId(),
				new AgendamentoRequestDTO(
						dto.servicoId(),
						dto.profissionalId(),
						dto.dataHoraInicio(),
						reservaOriginal.getObservacoes()
				),
				emailAutenticado
		);

		// 7. Recuperar a nova reserva para retornar o DTO completo.
		var novaReserva = agendamentoRepository
				.findByIdAndClienteId(
						novoAgendamento.id(),
						cliente.getId()
				)
				.orElseThrow(() -> new IllegalStateException(
						"A nova reserva não foi encontrada após o cadastro."
				));

		return AgendamentoClienteDTO.from(novaReserva);
	}
}
