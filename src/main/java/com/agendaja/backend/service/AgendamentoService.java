package com.agendaja.backend.service;

import com.agendaja.backend.builder.AgendamentoBuilder;
import com.agendaja.backend.dto.AgendamentoDetalhesDTO;
import com.agendaja.backend.dto.AgendamentoRequestDTO;
import com.agendaja.backend.dto.AgendamentoResponseDTO;
import com.agendaja.backend.event.AgendamentoCriadoEvent;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.ConcurrencyFailureException;

@Service
@RequiredArgsConstructor
public class AgendamentoService {
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    
    // Injeção para o Padrão Observer
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AgendamentoResponseDTO registrar(Long estabelecimentoId, AgendamentoRequestDTO dto, String emailCliente)
    {
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cliente não encontrado."));

        Profissional profissional = profissionalRepository.findByIdAndEstabelecimentoId(dto.profissionalId(), estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado no estabelecimento."));

        Servico servico = servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(dto.servicoId(), estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado."));

        // Utiliza o Builder
        Agendamento agendamento = new AgendamentoBuilder()
                .comCliente(cliente)
                .comProfissional(profissional)
                .comServico(servico)
                .comDataHoraInicio(dto.dataHoraInicio())
                .comObservacoes(dto.observacoes())
                .build();

        try {
            agendamento = agendamentoRepository.save(agendamento);
            
            // Força a inserção imediata no Postgres. Se houver sobreposição ou deadlock, ele lança exceção aqui.
            agendamentoRepository.flush();
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // Conflito de agenda ou impasse de concorrência
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O horário selecionado não está mais disponível. Por favor, escolha outro.");
        }

        // Dispara o evento (Observer) garantindo que a notificação ocorra de forma desacoplada
        eventPublisher.publishEvent(new AgendamentoCriadoEvent(agendamento));

        return AgendamentoResponseDTO.from(agendamento);
    }

    @Transactional(readOnly = true)
    public AgendamentoDetalhesDTO consultarDetalhes(Long estabelecimentoId, Long agendamentoId, String emailUsuario)
    {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado."));

        if (!agendamento.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não pertence a este estabelecimento.");

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        boolean isGestorDoEstabelecimento = "GESTOR".equals(usuario.getRole()) && 
                usuario.getEstabelecimento() != null && 
                usuario.getEstabelecimento().getId().equals(estabelecimentoId);
                
        boolean isClienteDoAgendamento = "CLIENTE".equals(usuario.getRole()) && 
                agendamento.getCliente().getId().equals(usuario.getId());

        if (!isGestorDoEstabelecimento && !isClienteDoAgendamento)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para visualizar este agendamento.");

        return AgendamentoDetalhesDTO.from(agendamento);
    }
}
