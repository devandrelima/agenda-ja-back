package com.agendaja.backend.service;

import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadoraDisponibilidadeServiceTest {
    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private CalculadoraDisponibilidadeService service;

    private Estabelecimento estabelecimento;
    private Servico servico;
    private Profissional profissional;
    private LocalDate dataTeste; // 2026-10-26 é uma Segunda-feira

    @BeforeEach
    void setup() {
        estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        servico = new Servico();
        servico.setId(10L);
        servico.setDuracaoMinutos(60);
        servico.setEstabelecimento(estabelecimento);

        profissional = new Profissional();
        profissional.setId(1L);
        profissional.setEstabelecimento(estabelecimento);
        profissional.setServicos(List.of(servico));
        profissional.setFolgas(new ArrayList<>());
        profissional.setJornadasTrabalho(new ArrayList<>());

        dataTeste = LocalDate.of(2026, 10, 26);
    }

    @Test
    void deveRetornarListaVaziaSeForDiaDeFolga() {
        var folga = new Folga();
        folga.setData(dataTeste);
        profissional.getFolgas().add(folga);

        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(servico));

        List<String> horarios = service.consultarHorarios(1L, 10L, 1L, dataTeste);

        assertTrue(horarios.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaSeNaoHouverJornadaNoDia() {
        // Adiciona jornada apenas na Terça-feira (2), mas vamos testar Segunda-feira (1)
        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(2); 
        profissional.getJornadasTrabalho().add(jornada);

        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(servico));

        List<String> horarios = service.consultarHorarios(1L, 10L, 1L, dataTeste);

        assertTrue(horarios.isEmpty());
    }

    @Test
    void deveCalcularHorariosLivresCorretamenteSemConflitos() {
        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(1); // Segunda-feira
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(12, 0));
        profissional.getJornadasTrabalho().add(jornada);

        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.buscarAgendamentosAtivosDoDia(anyLong(), any(), any(), any())).thenReturn(List.of());

        List<String> horarios = service.consultarHorarios(1L, 10L, 1L, dataTeste);

        // Turno 08:00 às 12:00, Serviço de 60 min, incrementos de 30 min.
        // Horários esperados: 08:00, 08:30, 09:00, 09:30, 10:00, 10:30, 11:00
        assertEquals(7, horarios.size());
        assertEquals("08:00", horarios.get(0));
        assertEquals("11:00", horarios.get(6));
        assertFalse(horarios.contains("11:30")); // Não cabe serviço de 60 min se terminar 12:00
    }

    @Test
    void deveRecortarBlocosOcupadosPelaMatematicaDeIntervalos() {
        var jornada = new JornadaTrabalho();
        jornada.setDiaSemana(1);
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(12, 0));
        profissional.getJornadasTrabalho().add(jornada);

        var agendamento = new Agendamento();
        agendamento.setDataHoraInicio(dataTeste.atTime(9, 0));
        agendamento.setDataHoraFim(dataTeste.atTime(10, 0));

        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.buscarAgendamentosAtivosDoDia(anyLong(), any(), any(), any()))
                .thenReturn(List.of(agendamento));

        List<String> horarios = service.consultarHorarios(1L, 10L, 1L, dataTeste);

        // Agendamento das 09:00 às 10:00
        // Livres antes: 08:00 (termina 09:00, ok). 08:30 (terminaria 09:30, colide).
        // Livres depois: 10:00, 10:30, 11:00.
        List<String> esperados = List.of("08:00", "10:00", "10:30", "11:00");
        
        assertEquals(esperados.size(), horarios.size());
        assertTrue(horarios.containsAll(esperados));
    }

    @Test
    void deveUnirAgendasDeMultiplosProfissionaisSeOmisso() {
        // Profissional 1: Trabalha de manhã
        var jornada1 = new JornadaTrabalho();
        jornada1.setDiaSemana(1);
        jornada1.setHoraInicio(LocalTime.of(8, 0));
        jornada1.setHoraFim(LocalTime.of(10, 0));
        profissional.getJornadasTrabalho().add(jornada1);

        // Profissional 2: Trabalha de tarde
        var prof2 = new Profissional();
        prof2.setId(2L);
        prof2.setEstabelecimento(estabelecimento);
        prof2.setServicos(List.of(servico));
        prof2.setFolgas(new ArrayList<>());
        
        var jornada2 = new JornadaTrabalho();
        jornada2.setDiaSemana(1);
        jornada2.setHoraInicio(LocalTime.of(14, 0));
        jornada2.setHoraFim(LocalTime.of(16, 0));
        prof2.setJornadasTrabalho(List.of(jornada2));

        when(profissionalRepository.findByEstabelecimentoIdAndServicosId(1L, 10L)).thenReturn(List.of(profissional, prof2));
        when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
        when(profissionalRepository.findByIdAndEstabelecimentoId(2L, 1L)).thenReturn(Optional.of(prof2));
        when(servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(10L, 1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.buscarAgendamentosAtivosDoDia(anyLong(), any(), any(), any())).thenReturn(List.of());

        // Chamando com profissionalId = null
        List<String> horarios = service.consultarHorarios(1L, 10L, null, dataTeste);

        List<String> esperados = List.of("08:00", "08:30", "09:00", "14:00", "14:30", "15:00");
        assertEquals(esperados.size(), horarios.size());
        assertTrue(horarios.containsAll(esperados));
    }
}
