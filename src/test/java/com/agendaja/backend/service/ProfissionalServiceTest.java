package com.agendaja.backend.service;

import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Profissional;
import com.agendaja.backend.model.Servico;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {
    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private EstabelecimentoRepository estabelecimentoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private EstabelecimentoAuthorizationService authorizationService;

    @InjectMocks
    private ProfissionalService profissionalService;

    @Test
    void criar_deveCriarERetornarProfissional() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var servico = new Servico();
        servico.setId(10L);
        servico.setEstabelecimento(estabelecimento);

        var dto = new ProfissionalRequestDTO("Lucas Mendes", List.of("Corte"), List.of(10L), List.of(1), List.of("2026-10-15"));

        var profissionalSalvo = new Profissional();
        profissionalSalvo.setId(1L);
        profissionalSalvo.setNome("Lucas Mendes");
        profissionalSalvo.setEstabelecimento(estabelecimento);
        profissionalSalvo.setEspecialidades(List.of("Corte"));
        profissionalSalvo.setServicos(List.of(servico));

        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(estabelecimento));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servico));
        when(profissionalRepository.save(any(Profissional.class))).thenReturn(profissionalSalvo);

        var response = profissionalService.criar(1L, dto, "gestor@email.com");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Lucas Mendes", response.nome());

        verify(authorizationService).validarAcesso("gestor@email.com", 1L);
    }

    @Test
    void criar_deveLancarExcecaoSeServicoNaoForDoEstabelecimento() {
        var estabelecimento1 = new Estabelecimento();
        estabelecimento1.setId(1L);

        var estabelecimento2 = new Estabelecimento();
        estabelecimento2.setId(2L);

        var servicoOutroEstabelecimento = new Servico();
        servicoOutroEstabelecimento.setId(10L);
        servicoOutroEstabelecimento.setEstabelecimento(estabelecimento2);

        var dto = new ProfissionalRequestDTO("Lucas", List.of("Corte"), List.of(10L), List.of(1), List.of());

        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(estabelecimento1));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servicoOutroEstabelecimento));

        assertThrows(ResponseStatusException.class, () -> profissionalService.criar(1L, dto, "gestor@email.com"));

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    void editar_deveAtualizarERetornarProfissional() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var servico = new Servico();
        servico.setId(10L);
        servico.setEstabelecimento(estabelecimento);

        var profissionalExistente = new Profissional();
        profissionalExistente.setId(1L);
        profissionalExistente.setNome("Nome Antigo");
        profissionalExistente.setEstabelecimento(estabelecimento);

        var dto = new ProfissionalRequestDTO("Nome Novo", List.of("Barba"), List.of(10L), List.of(2), List.of());

        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissionalExistente));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servico));

        var response = profissionalService.editar(1L, 1L, dto, "gestor@email.com");

        assertEquals("Nome Novo", response.nome());
        assertEquals(List.of("Barba"), response.especialidades());

        verify(authorizationService).validarAcesso("gestor@email.com", 1L);
    }

    @Test
    void listarPorServicoPublico_deveRetornarProfissionaisDoServico() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Lucas");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setEspecialidades(List.of("Corte"));

        when(profissionalRepository.findByEstabelecimentoIdAndServicosId(1L, 10L))
                .thenReturn(List.of(profissional));

        var response = profissionalService.listarPorServicoPublico(1L, 10L);

        assertEquals(1, response.size());
        assertEquals("Lucas", response.get(0).nome());
        // Garante que o service de autorização (segurança) não foi chamado em rotas públicas
        verifyNoInteractions(authorizationService); 
    }

    @Test
    void sortearProfissionalPorServico_deveRetornarUmProfissional() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Lucas");
        profissional.setEstabelecimento(estabelecimento);

        when(profissionalRepository.findByEstabelecimentoIdAndServicosId(1L, 10L))
                .thenReturn(List.of(profissional));

        var response = profissionalService.sortearProfissionalPorServico(1L, 10L);

        assertNotNull(response);
        assertEquals("Lucas", response.nome());
    }

    @Test
    void sortearProfissionalPorServico_deveLancarExcecaoSeNaoHouverProfissional() {
        when(profissionalRepository.findByEstabelecimentoIdAndServicosId(1L, 10L))
                .thenReturn(List.of());

        assertThrows(ResponseStatusException.class, 
                () -> profissionalService.sortearProfissionalPorServico(1L, 10L));
    }
}
