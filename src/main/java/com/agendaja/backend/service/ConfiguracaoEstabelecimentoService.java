package com.agendaja.backend.service;

import com.agendaja.backend.dto.HorarioFuncionamentoDTO;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracaoEstabelecimentoService {
    private final EstabelecimentoRepository repository;
    private final EstabelecimentoAuthorizationService authorizationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Estabelecimento autorizado(Long id, String email) {
        authorizationService.validarAcesso(email, id);
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado."));
    }

    @Transactional
    public void salvarDados(Long id, String email, String nome, String categoria, String localizacao) {
        if (vazioOuLongo(nome, 100) || vazioOuLongo(categoria, 50) || vazioOuLongo(localizacao, 255))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do negócio inválidos.");
        var estabelecimento = autorizado(id, email);
        estabelecimento.setNome(nome.strip());
        estabelecimento.setCategoria(categoria.strip());
        estabelecimento.setLocalizacao(localizacao.strip());
    }

    private boolean vazioOuLongo(String valor, int limite) {
        return valor == null || valor.isBlank() || valor.strip().length() > limite;
    }

    @Transactional
    public void salvarCapa(Long id, String email, String imagemUrl) {
        if (imagemUrl == null || !imagemUrl.startsWith("data:image/jpeg;base64,") || imagemUrl.length() > 3_000_000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem de capa inválida.");
        autorizado(id, email).setImagemUrl(imagemUrl);
    }

    public List<HorarioFuncionamentoDTO> lerHorarios(Estabelecimento estabelecimento) {
        if (estabelecimento.getHorariosFuncionamento() == null)
            return List.of(new HorarioFuncionamentoDTO(List.of()), new HorarioFuncionamentoDTO(List.of()),
                    new HorarioFuncionamentoDTO(List.of()), new HorarioFuncionamentoDTO(List.of()),
                    new HorarioFuncionamentoDTO(List.of()), new HorarioFuncionamentoDTO(List.of()),
                    new HorarioFuncionamentoDTO(List.of()));
        try {
            return objectMapper.readValue(estabelecimento.getHorariosFuncionamento(), new TypeReference<>() {});
        } catch (Exception erro) {
            throw new IllegalStateException("Horários de funcionamento inválidos no banco.", erro);
        }
    }

    @Transactional
    public void salvarHorarios(Long id, String email, List<HorarioFuncionamentoDTO> horarios) {
        if (horarios == null || horarios.size() != 7)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os sete dias da semana.");
        for (var dia : horarios) {
            if (dia == null || dia.turnos() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horários inválidos.");
            var ordenados = new ArrayList<>(dia.turnos());
            for (var turno : ordenados) {
                try {
                    if (turno == null || !turno.inicio().matches("\\d{2}:\\d{2}") || !turno.fim().matches("\\d{2}:\\d{2}")
                            || !LocalTime.parse(turno.inicio()).isBefore(LocalTime.parse(turno.fim())))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno inválido.");
                } catch (DateTimeParseException | NullPointerException erro) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno inválido.");
                }
            }
            ordenados.sort((primeiro, segundo) -> primeiro.inicio().compareTo(segundo.inicio()));
            for (int i = 1; i < ordenados.size(); i++)
                if (ordenados.get(i).inicio().compareTo(ordenados.get(i - 1).fim()) < 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turnos sobrepostos.");
        }
        var estabelecimento = autorizado(id, email);
        try {
            estabelecimento.setHorariosFuncionamento(objectMapper.writeValueAsString(horarios));
        } catch (Exception erro) {
            throw new IllegalStateException("Não foi possível salvar os horários.", erro);
        }
    }
}
