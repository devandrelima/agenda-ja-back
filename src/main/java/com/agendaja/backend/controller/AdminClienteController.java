package com.agendaja.backend.controller;

import com.agendaja.backend.dto.ClienteHistoricoDTO;
import com.agendaja.backend.dto.ClienteResumoDTO;
import com.agendaja.backend.service.ClienteGestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin/estabelecimentos/{estabelecimentoId}/clientes")
@RequiredArgsConstructor
@Tag(name = "Admin - Clientes", description = "Consulta do histórico e frequência de clientes pelo gestor")
public class AdminClienteController {
    private final ClienteGestorService clienteGestorService;

    @Operation(
            summary = "Listar clientes do estabelecimento",
            description = "Retorna uma lista resumida de todos os clientes que já concluíram ou têm agendamentos confirmados no estabelecimento, ordenada pela data da última visita."
    )
    @GetMapping
    public List<ClienteResumoDTO> listarClientes(
            @PathVariable Long estabelecimentoId,
            Authentication authentication
    ) {
        return clienteGestorService.listarClientes(estabelecimentoId, authentication.getName());
    }

    @Operation(
            summary = "Consultar histórico do cliente",
            description = "Retorna os detalhes de um cliente e todo o seu histórico de agendamentos dentro deste estabelecimento específico, incluindo o cálculo do serviço favorito."
    )
    @GetMapping("/{clienteId}")
    public ClienteHistoricoDTO consultarHistorico(
            @PathVariable Long estabelecimentoId,
            @PathVariable Long clienteId,
            Authentication authentication
    ) {
        return clienteGestorService.consultarHistorico(estabelecimentoId, clienteId, authentication.getName());
    }
}
