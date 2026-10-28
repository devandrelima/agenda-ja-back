package com.agendaja.backend.service;

import com.agendaja.backend.dto.AvaliacaoRequestDTO;
import com.agendaja.backend.dto.AvaliacaoResponseDTO;
import com.agendaja.backend.model.Avaliacao;
import com.agendaja.backend.repository.AvaliacaoRepository;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

	private final AvaliacaoRepository avaliacaoRepository;
	private final EstabelecimentoRepository estabelecimentoRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional
	public AvaliacaoResponseDTO publicar(
			Long estabelecimentoId,
			AvaliacaoRequestDTO dto,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);
		var estabelecimento = estabelecimentoRepository
				.findById(estabelecimentoId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Estabelecimento não encontrado."
				));

		if (avaliacaoRepository.existsByClienteIdAndEstabelecimentoId(
				cliente.getId(),
				estabelecimentoId
		)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Você já avaliou este estabelecimento."
			);
		}

		var avaliacao = new Avaliacao();

		avaliacao.setCliente(cliente);
		avaliacao.setEstabelecimento(estabelecimento);
		avaliacao.setNota(dto.nota());
		avaliacao.setComentario(dto.comentario());

		var avaliacaoSalva = avaliacaoRepository.saveAndFlush(avaliacao);

		return AvaliacaoResponseDTO.from(avaliacaoSalva);
	}

	@Transactional
	public AvaliacaoResponseDTO editar(
			Long estabelecimentoId,
			AvaliacaoRequestDTO dto,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);
		var avaliacao = buscarAvaliacaoDoCliente(
				cliente.getId(),
				estabelecimentoId
		);

		avaliacao.setNota(dto.nota());
		avaliacao.setComentario(dto.comentario());

		avaliacaoRepository.flush();

		return AvaliacaoResponseDTO.from(avaliacao);
	}

	@Transactional
	public void excluir(
			Long estabelecimentoId,
			String emailAutenticado
	) {
		var cliente = buscarCliente(emailAutenticado);
		var avaliacao = buscarAvaliacaoDoCliente(
				cliente.getId(),
				estabelecimentoId
		);

		avaliacaoRepository.delete(avaliacao);
	}

	private com.agendaja.backend.model.Usuario buscarCliente(
			String emailAutenticado
	) {
		var usuario = usuarioRepository.findByEmail(emailAutenticado)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED,
						"Usuário autenticado não encontrado."
				));

		if (!"CLIENTE".equals(usuario.getRole())) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Apenas clientes podem gerenciar avaliações."
			);
		}

		return usuario;
	}

	private Avaliacao buscarAvaliacaoDoCliente(
			Long clienteId,
			Long estabelecimentoId
	) {
		return avaliacaoRepository
				.findByClienteIdAndEstabelecimentoId(
						clienteId,
						estabelecimentoId
				)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Avaliação não encontrada."
				));
	}
}
