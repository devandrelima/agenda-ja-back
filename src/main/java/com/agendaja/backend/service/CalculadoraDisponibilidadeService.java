package com.agendaja.backend.service;

import com.agendaja.backend.model.*;
import com.agendaja.backend.repository.AgendamentoRepository;
import com.agendaja.backend.repository.ProfissionalRepository;
import com.agendaja.backend.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculadoraDisponibilidadeService {
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    // Record auxiliar para representar um bloco de tempo
    private record Intervalo(LocalTime inicio, LocalTime fim) {}

    @Transactional(readOnly = true)
    public List<String> consultarHorarios(Long estabelecimentoId, Long servicoId, Long profissionalId, LocalDate data)
    {
        List<LocalTime> horarios;

        horarios = calcularParaUmProfissional(estabelecimentoId, profissionalId, servicoId, data);

        // Converte os horários de LocalTime para String "HH:mm" para facilitar a leitura no frontend
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return horarios.stream().map(h -> h.format(formatter)).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalTime> calcularParaUmProfissional(Long estabelecimentoId, Long profissionalId, Long servicoId, LocalDate data)
    {
        Profissional profissional = profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));

        Servico servico = servicoRepository.findByIdAndEstabelecimentoIdAndAtivoTrue(servicoId, estabelecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado."));

        if (!profissional.getServicos().contains(servico))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O profissional não realiza este serviço.");

        // Verifica Folgas
        if (profissional.getFolgas().stream().anyMatch(folga -> folga.getData().equals(data))) return List.of();

        // Transforma Jornadas em Blocos Livres Iniciais
        int diaSemana = data.getDayOfWeek().getValue();
        List<Intervalo> blocosLivres = new ArrayList<>(profissional.getJornadasTrabalho().stream()
                .filter(jornada -> jornada.getDiaSemana() == diaSemana)
                .map(j -> new Intervalo(j.getHoraInicio(), j.getHoraFim()))
                .toList());
        if (blocosLivres.isEmpty()) return List.of();

        // Busca Agendamentos e recorta os Blocos Livres
        List<Agendamento> agendamentos = agendamentoRepository.buscarAgendamentosAtivosDoDia(
                profissionalId, data.atStartOfDay(), data.atTime(LocalTime.MAX), StatusAgendamento.CANCELADO);

        for (Agendamento agendamento : agendamentos) {
            Intervalo ocupado = new Intervalo(
                    agendamento.getDataHoraInicio().toLocalTime(),
                    agendamento.getDataHoraFim().toLocalTime()
            );
            blocosLivres = subtrairIntervalo(blocosLivres, ocupado);
        }

        // Gera os horários baseados na duração do serviço dentro dos blocos realmente livres
        List<LocalTime> horariosDisponiveis = new ArrayList<>();
        int duracao = servico.getDuracaoMinutos();
        int incrementoGrade = 30; // Assim a interface UI exibe horários a cada 30 min (ex: 08:00, 08:30)

        for (Intervalo bloco : blocosLivres) {
            // Garante que a grade comece em um número redondo (ex: se o bloco livre começa 09:15, o primeiro horário será 09:30)
            LocalTime slotAtual = arredondarParaProximoIncremento(bloco.inicio(), incrementoGrade);
            
            while (!slotAtual.plusMinutes(duracao).isAfter(bloco.fim())) {
                horariosDisponiveis.add(slotAtual);
                slotAtual = slotAtual.plusMinutes(incrementoGrade);
            }
        }

        return horariosDisponiveis.stream().distinct().sorted().toList();
    }

    /**
     * Algoritmo principal da Matemática de Intervalos
     * Compara um bloco ocupado com todos os blocos livres e devolve os resíduos limpos
     */
    private List<Intervalo> subtrairIntervalo(List<Intervalo> livres, Intervalo ocupado)
    {
        List<Intervalo> result = new ArrayList<>();
        
        for (Intervalo livre : livres) {
            // Sem interseção (Ocupado totalmente antes ou totalmente depois do Livre)
            if (!ocupado.fim().isAfter(livre.inicio()) || !ocupado.inicio().isBefore(livre.fim())) result.add(livre);

            // Ocupado engole o Livre completamente (O bloco livre some)
            else if (!ocupado.inicio().isAfter(livre.inicio()) && !ocupado.fim().isBefore(livre.fim())) continue;

            // Ocupado divide o Livre no meio (Gera dois novos blocos livres residuais)
            else if (ocupado.inicio().isAfter(livre.inicio()) && ocupado.fim().isBefore(livre.fim())) {
                result.add(new Intervalo(livre.inicio(), ocupado.inicio()));
                result.add(new Intervalo(ocupado.fim(), livre.fim()));
            }

            // Ocupado morde apenas o início do Livre
            else if (!ocupado.inicio().isAfter(livre.inicio()) && ocupado.fim().isBefore(livre.fim())) 
                result.add(new Intervalo(ocupado.fim(), livre.fim()));
            
            // Ocupado morde apenas o fim do Livre
            else if (ocupado.inicio().isAfter(livre.inicio()) && !ocupado.fim().isBefore(livre.fim()))
                result.add(new Intervalo(livre.inicio(), ocupado.inicio()));
        }

        return result;
    }

    private LocalTime arredondarParaProximoIncremento(LocalTime hora, int incrementoMinutos)
    {
        int minutos = hora.getMinute();
        int resto = minutos % incrementoMinutos;
        
        if (resto == 0) return hora;

        return hora.plusMinutes(incrementoMinutos - resto);
    }
}
