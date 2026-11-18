package com.agendaja.backend.service;

import com.agendaja.backend.dto.ProfissionalRequestDTO;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.model.Folga;
import com.agendaja.backend.model.JornadaTrabalho;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
        servico.setNome("Corte");
        servico.setEstabelecimento(estabelecimento);

        var dto = new ProfissionalRequestDTO("Lucas Mendes", List.of(10L), List.of(1), List.of("2026-10-15"));

        var profissionalSalvo = new Profissional();
        profissionalSalvo.setId(1L);
        profissionalSalvo.setNome("Lucas Mendes");
        profissionalSalvo.setEstabelecimento(estabelecimento);
        profissionalSalvo.setServicos(List.of(servico));

        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(estabelecimento));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servico));
        when(profissionalRepository.save(any(Profissional.class))).thenReturn(profissionalSalvo);

        var response = profissionalService.criar(1L, dto, "gestor@email.com");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Lucas Mendes", response.nome());
        assertEquals(List.of("Corte"), response.especialidades());

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

        var dto = new ProfissionalRequestDTO("Lucas", List.of(10L), List.of(1), List.of());

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
        servico.setNome("Barba");
        servico.setEstabelecimento(estabelecimento);

        var profissionalExistente = new Profissional();
        profissionalExistente.setId(1L);
        profissionalExistente.setNome("Nome Antigo");
        profissionalExistente.setEstabelecimento(estabelecimento);

        var dto = new ProfissionalRequestDTO("Nome Novo", List.of(10L), List.of(2), List.of());

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L)).thenReturn(Optional.of(profissionalExistente));
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

        var servico = new Servico();
        servico.setId(10L);
        servico.setNome("Corte");
        servico.setEstabelecimento(estabelecimento);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Lucas");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setServicos(List.of(servico));

        when(profissionalRepository.findByEstabelecimentoIdAndServicosIdAndAtivoTrue(1L, 10L))
                .thenReturn(List.of(profissional));

        var response = profissionalService.listarPorServicoPublico(1L, 10L);

        assertEquals(1, response.size());
        assertEquals("Lucas", response.get(0).nome());
        verifyNoInteractions(authorizationService); 
    }

    @Test
    void desativar_deveDesativarProfissional() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setAtivo(true);
        profissional.setEstabelecimento(estabelecimento);

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L))
                .thenReturn(Optional.of(profissional));

        profissionalService.desativar(1L, 1L, "gestor@email.com");

        assertFalse(profissional.isAtivo());
        verify(authorizationService).validarAcesso("gestor@email.com", 1L);
        verify(profissionalRepository, never()).delete(any()); // Garante que é soft delete
    }

    @Test
    void criar_deveRetornar404QuandoEstabelecimentoNaoExiste() {
        var dto = new ProfissionalRequestDTO("Lucas", List.of(10L), null, null);
        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class,
                () -> profissionalService.criar(1L, dto, "gestor@email.com"));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void criar_deveFuncionarComDiasDeAtendimentoEFolgaNulos() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var servico = new Servico();
        servico.setId(10L);
        servico.setNome("Corte");
        servico.setEstabelecimento(estabelecimento);

        var dto = new ProfissionalRequestDTO("Lucas", List.of(10L), null, null);
        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(estabelecimento));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servico));
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(inv -> {
            Profissional p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var response = profissionalService.criar(1L, dto, "gestor@email.com");

        assertEquals("Lucas", response.nome());
        assertTrue(response.diasDeAtendimento().isEmpty());
        assertTrue(response.diasDeFolga().isEmpty());
    }

    @Test
    void criar_deveLancar400QuandoAlgumServicoNaoForEncontrado() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var servico = new Servico();
        servico.setId(10L);
        servico.setEstabelecimento(estabelecimento);

        // Solicita 2 serviços, mas apenas 1 existe no banco
        var dto = new ProfissionalRequestDTO("Lucas", List.of(10L, 99L), List.of(1), List.of());
        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(estabelecimento));
        when(servicoRepository.findAllById(List.of(10L, 99L))).thenReturn(List.of(servico));

        var erro = assertThrows(ResponseStatusException.class,
                () -> profissionalService.criar(1L, dto, "gestor@email.com"));

        assertEquals(400, erro.getStatusCode().value());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    void listarEConsultar_devemRetornarProfissionaisOu404() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Ana");
        profissional.setEstabelecimento(estabelecimento);

        when(profissionalRepository.findByEstabelecimentoIdAndAtivoTrue(1L))
                .thenReturn(List.of(profissional));
        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L))
                .thenReturn(Optional.of(profissional));
        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(99L, 1L))
                .thenReturn(Optional.empty());

        assertEquals(1, profissionalService.listar(1L, "gestor@email.com").size());
        assertEquals("Ana", profissionalService.consultar(1L, 1L, "gestor@email.com").nome());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> profissionalService.consultar(1L, 99L, "gestor@email.com")).getStatusCode().value());
    }

    @Test
    void editar_deveManterRemoverEAdicionarJornadasEFolgasETratarNulos() {
        var estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        var servico = new Servico();
        servico.setId(10L);
        servico.setNome("Corte");
        servico.setEstabelecimento(estabelecimento);

        var profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Ana");
        profissional.setEstabelecimento(estabelecimento);
        profissional.setJornadasTrabalho(new ArrayList<>());
        profissional.setFolgas(new ArrayList<>());

        // Jornadas existentes: dia 1 (será mantido) e dia 2 (será removido)
        var j1 = new JornadaTrabalho();
        j1.setDiaSemana(1);
        j1.setHoraInicio(LocalTime.of(8, 0));
        j1.setHoraFim(LocalTime.of(12, 0));
        j1.setProfissional(profissional);

        var j2 = new JornadaTrabalho();
        j2.setDiaSemana(2);
        j2.setHoraInicio(LocalTime.of(8, 0));
        j2.setHoraFim(LocalTime.of(12, 0));
        j2.setProfissional(profissional);
        profissional.getJornadasTrabalho().addAll(List.of(j1, j2));

        // Folgas existentes: 2026-12-25 (mantida) e 2026-11-02 (removida)
        var f1 = new Folga();
        f1.setData(LocalDate.of(2026, 12, 25));
        f1.setProfissional(profissional);

        var f2 = new Folga();
        f2.setData(LocalDate.of(2026, 11, 2));
        f2.setProfissional(profissional);
        profissional.getFolgas().addAll(List.of(f1, f2));

        when(profissionalRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(1L, 1L))
                .thenReturn(Optional.of(profissional));
        when(servicoRepository.findAllById(List.of(10L))).thenReturn(List.of(servico));

        // Mantém dia 1 e folga 2026-12-25; remove dia 2 e folga 2026-11-02; adiciona dia 3 e folga 2026-12-31
        var dtoMisto = new ProfissionalRequestDTO(
                "Ana Editada",
                List.of(10L),
                List.of(1, 3),
                List.of("2026-12-25", "2026-12-31")
        );
        var resposta1 = profissionalService.editar(1L, 1L, dtoMisto, "gestor@email.com");
        assertEquals(List.of(1, 3), resposta1.diasDeAtendimento());
        assertEquals(List.of("2026-12-25", "2026-12-31"), resposta1.diasDeFolga());

        // Com listas nulas, preserva jornadas e folgas existentes
        var dtoNulo = new ProfissionalRequestDTO("Ana Editada", List.of(10L), null, null);
        var resposta2 = profissionalService.editar(1L, 1L, dtoNulo, "gestor@email.com");
        assertEquals(List.of(1, 3), resposta2.diasDeAtendimento());
        assertEquals(List.of("2026-12-25", "2026-12-31"), resposta2.diasDeFolga());
    }
}
