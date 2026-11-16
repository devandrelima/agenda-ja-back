package com.agendaja.backend.service;

import com.agendaja.backend.dto.DashboardResumoDTO;
import com.agendaja.backend.dto.ProximoAtendimentoDTO;
import com.agendaja.backend.model.StatusAgendamento;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final AgendamentoRepository agendamentoRepository;
	private final EstabelecimentoAuthorizationService authorizationService;

	@Transactional(readOnly = true)
	public DashboardResumoDTO consultarDashboard(
			Long estabelecimentoId,
			LocalDate data,
			String emailGestor
	) {
		authorizationService.validarAcesso(emailGestor, estabelecimentoId);

		if (data == null) {
			data = LocalDate.now();
		}

		LocalDateTime inicioDia = data.atStartOfDay();
		LocalDateTime fimDia = data.plusDays(1).atStartOfDay();

		var resumo = agendamentoRepository.buscarResumoDashboard(
				estabelecimentoId,
				inicioDia,
				fimDia,
				StatusAgendamento.CONFIRMADO,
				StatusAgendamento.AGUARDANDO_CONFIRMACAO,
				StatusAgendamento.CANCELADO,
				StatusAgendamento.CONCLUIDO
		);

		LocalDateTime agora = LocalDateTime.now();

		// Para uma data futura, todos os horários do dia ainda são futuros.
		// Para hoje, consideramos apenas os horários a partir do momento atual.
		LocalDateTime aPartirDe = data.isAfter(LocalDate.now())
				? inicioDia
				: agora;

		List<ProximoAtendimentoDTO> proximosAtendimentos =
				agendamentoRepository.buscarProximosAtendimentos(
								estabelecimentoId,
								aPartirDe,
								fimDia,
								List.of(
										StatusAgendamento.AGUARDANDO_CONFIRMACAO,
										StatusAgendamento.CONFIRMADO
								)
						)
						.stream()
						.map(ProximoAtendimentoDTO::from)
						.toList();

		return new DashboardResumoDTO(
				data,
				resumo.getTotalReservas(),
				resumo.getConfirmados(),
				resumo.getPendentes(),
				resumo.getCancelados(),
				resumo.getConcluidos(),
				resumo.getFaturamentoPrevisto(),
				proximosAtendimentos
		);
	}
}