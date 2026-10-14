
package com.agendaja.backend.service;

import com.agendaja.backend.dto.ServicoRequestDTO;
import com.agendaja.backend.dto.ServicoResponseDTO;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoService {

	private final ServicoRepository servicoRepository;
	private final EstabelecimentoRepository estabelecimentoRepository;
	private final EstabelecimentoAuthorizationService authorizationService;

	@Transactional
	public ServicoResponseDTO criar(
			Long estabelecimentoId,
			ServicoRequestDTO dto,
			String emailGestor
	) {
		authorizationService.validarAcesso(
				emailGestor, estabelecimentoId
		);

		var estabelecimento = estabelecimentoRepository
				.findById(estabelecimentoId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estabelecimento não encontrado"
				));

		var servico = new Servico();
		servico.setNome(dto.nome());
		servico.setDuracaoMinutos(dto.duracaoMinutos());
		servico.setValor(dto.valor());
		servico.setEstabelecimento(estabelecimento);

		return ServicoResponseDTO.from(
				servicoRepository.save(servico)
		);
	}

	@Transactional(readOnly = true)
	public List<ServicoResponseDTO> listar(
			Long estabelecimentoId,
			String emailGestor
	) {
		authorizationService.validarAcesso(
				emailGestor, estabelecimentoId
		);

		return servicoRepository
				.findByEstabelecimentoIdAndAtivoTrue(estabelecimentoId)
				.stream()
				.map(ServicoResponseDTO::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ServicoResponseDTO consultar(
			Long estabelecimentoId,
			Long servicoId,
			String emailGestor
	) {
		authorizationService.validarAcesso(
				emailGestor, estabelecimentoId
		);

		return ServicoResponseDTO.from(
				buscarServico(estabelecimentoId, servicoId)
		);
	}

	@Transactional
	public ServicoResponseDTO editar(
			Long estabelecimentoId,
			Long servicoId,
			ServicoRequestDTO dto,
			String emailGestor
	) {
		authorizationService.validarAcesso(
				emailGestor, estabelecimentoId
		);

		var servico = buscarServico(estabelecimentoId, servicoId);

		servico.setNome(dto.nome());
		servico.setDuracaoMinutos(dto.duracaoMinutos());
		servico.setValor(dto.valor());

		return ServicoResponseDTO.from(servico);
	}

	private Servico buscarServico(
			Long estabelecimentoId,
			Long servicoId
	) {
		return servicoRepository
				.findByIdAndEstabelecimentoIdAndAtivoTrue(
						servicoId, estabelecimentoId
				)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Serviço não encontrado"
				));
	}

	@Transactional
	public void desativar(
			Long estabelecimentoId,
			Long servicoId,
			String emailGestor
	) {
		authorizationService.validarAcesso(
				emailGestor, estabelecimentoId
		);

		var servico = buscarServico(
				estabelecimentoId, servicoId
		);

		servico.setAtivo(false);
	}

}
