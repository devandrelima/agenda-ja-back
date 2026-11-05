package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoClienteService {

	private final AgendamentoRepository agendamentoRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional(readOnly = true)
	public List<AgendamentoClienteDTO> listar(
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);

		return agendamentoRepository
				.findByClienteIdOrderByDataHoraInicioAsc(
						cliente.getId()
				)
				.stream()
				.map(AgendamentoClienteDTO::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AgendamentoClienteDTO consultar(
			Long agendamentoId,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);

		return AgendamentoClienteDTO.from(
				buscarAgendamentoDoCliente(
						agendamentoId,
						cliente.getId()
				)
		);
	}

	@Transactional
	public AgendamentoClienteDTO confirmar(
			Long agendamentoId,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);

		int alterados = agendamentoRepository.confirmarSePendente(
				agendamentoId,
				cliente.getId(),
				StatusAgendamento.AGUARDANDO_CONFIRMACAO,
				StatusAgendamento.CONFIRMADO
		);

		if (alterados == 0) {
			// Não revelar uma reserva de outro usuário.
			buscarAgendamentoDoCliente(
					agendamentoId,
					cliente.getId()
			);

			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"A reserva não está aguardando confirmação."
			);
		}

		return AgendamentoClienteDTO.from(
				buscarAgendamentoDoCliente(
						agendamentoId,
						cliente.getId()
				)
		);
	}

	private com.agendaja.backend.model.Agendamento
	buscarAgendamentoDoCliente(
			Long id,
			Long clienteId
	) {
		return agendamentoRepository
				.findByIdAndClienteId(id, clienteId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Agendamento não encontrado."
				));
	}

	private Usuario buscarCliente(String emailAutenticado) {
		var usuario = usuarioRepository
				.findByEmail(emailAutenticado)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED,
						"Sessão inválida ou expirada."
				));

		if (!"CLIENTE".equals(usuario.getRole())) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Somente clientes podem acessar seus agendamentos."
			);
		}

		return usuario;
	}

	@Transactional
	public AgendamentoClienteDTO cancelar(
			Long agendamentoId,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);

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
			// Retorna 404 se não existir ou pertencer a outro cliente.
			buscarAgendamentoDoCliente(
					agendamentoId,
					cliente.getId()
			);

			// Se existe e pertence ao cliente, o status é inválido.
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Este agendamento não pode mais ser cancelado."
			);
		}

		return AgendamentoClienteDTO.from(
				buscarAgendamentoDoCliente(
						agendamentoId,
						cliente.getId()
				)
		);
	}
}
