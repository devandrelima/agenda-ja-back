package com.agendaja.backend.service;

import com.agendaja.backend.dto.AgendamentoHistoricoDTO;
import com.agendaja.backend.dto.ClienteHistoricoDTO;
import com.agendaja.backend.dto.ClienteResumoDTO;
import com.agendaja.backend.model.Agendamento;
import com.agendaja.backend.model.Usuario;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.UsuarioRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteGestorService {
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<ClienteResumoDTO> listarClientes(Long estabelecimentoId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        return agendamentoRepository.buscarResumoClientesPorEstabelecimento(estabelecimentoId)
                .stream()
                .map(ClienteResumoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteHistoricoDTO consultarHistorico(Long estabelecimentoId, Long clienteId, String emailGestor)
    {
        authorizationService.validarAcesso(emailGestor, estabelecimentoId);

        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        List<Agendamento> agendamentos = agendamentoRepository
                .buscarHistoricoDoClienteNoEstabelecimento(estabelecimentoId, clienteId);

        String servicoFrequente = calcularServicoMaisFrequente(agendamentos);

        List<AgendamentoHistoricoDTO> historicoDto = agendamentos.stream()
                .map(AgendamentoHistoricoDTO::from)
                .toList();

        return new ClienteHistoricoDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                servicoFrequente,
                historicoDto
        );
    }

    private String calcularServicoMaisFrequente(List<Agendamento> agendamentos)
    {
        if (agendamentos == null || agendamentos.isEmpty()) return "Nenhum";

        // Agrupa os agendamentos pelo nome do serviço e conta as ocorrências, pegando o que tiver maior contagem
        return agendamentos.stream()
                .collect(Collectors.groupingBy(a -> a.getServico().getNome(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .orElse("Nenhum");
    }
}
