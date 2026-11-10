package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoClienteDTO;
import com.agendaja.backend.event.AgendamentoStatusAlteradoEvent;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

	// Injeção do publicador de eventos
	private final ApplicationEventPublisher applicationEventPublisher;

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
			buscarAgendamentoDoCliente(agendamentoId, cliente.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserva não está aguardando confirmação.");
		}

		var agendamentoAtualizado = buscarAgendamentoDoCliente(agendamentoId, cliente.getId());

		// CORREÇÃO: Atualizando manualmente o status na memória do Hibernate
		agendamentoAtualizado.setStatus(StatusAgendamento.CONFIRMADO);

		applicationEventPublisher.publishEvent(
				new AgendamentoStatusAlteradoEvent(agendamentoAtualizado, StatusAgendamento.AGUARDANDO_CONFIRMACAO)
		);

		return AgendamentoClienteDTO.from(agendamentoAtualizado);
	}

	private com.agendaja.backend.model.Agendamento buscarAgendamentoDoCliente(
			Long id,
			Long clienteId
	) {
		var agendamento = agendamentoRepository
				.findByIdAndClienteId(id, clienteId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Agendamento não encontrado."
				));

		// CORREÇÃO: "Toca" nos atributos para forçar o Hibernate a carregar os dados (Lazy Loading)
		// para a memória ANTES da transação acabar. Isso salva a vida do nosso Observer assíncrono
		if (agendamento.getCliente() != null) {
			agendamento.getCliente().getNome();
			agendamento.getCliente().getEmail();
		}
		if (agendamento.getServico() != null) {
			agendamento.getServico().getNome();
		}
		if (agendamento.getProfissional() != null) {
			agendamento.getProfissional().getNome();
		}

		return agendamento;
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

		var agendamentoExistente = buscarAgendamentoDoCliente(agendamentoId, cliente.getId());
		StatusAgendamento statusAnterior = agendamentoExistente.getStatus();

		int alterados = agendamentoRepository.cancelarSePermitido(
				agendamentoId,
				cliente.getId(),
				List.of(StatusAgendamento.AGUARDANDO_CONFIRMACAO, StatusAgendamento.CONFIRMADO),
				StatusAgendamento.CANCELADO
		);

		if (alterados == 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Este agendamento não pode mais ser cancelado.");
		}

		var agendamentoAtualizado = buscarAgendamentoDoCliente(agendamentoId, cliente.getId());

		// CORREÇÃO: Atualizando manualmente o status na memória do Hibernate
		agendamentoAtualizado.setStatus(StatusAgendamento.CANCELADO);

		applicationEventPublisher.publishEvent(
				new AgendamentoStatusAlteradoEvent(agendamentoAtualizado, statusAnterior)
		);

		return AgendamentoClienteDTO.from(agendamentoAtualizado);
	}
}